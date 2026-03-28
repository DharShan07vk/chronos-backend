package com.chronos.chronos.service;

import com.chronos.chronos.dto.CapsuleRequest;
import com.chronos.chronos.dto.CapsuleResponse;
import com.chronos.chronos.exception.ApiException;
import com.chronos.chronos.model.CapsuleModel;
import com.chronos.chronos.model.CapsuleShareModel;
import com.chronos.chronos.model.CapsuleShareStatus;
import com.chronos.chronos.model.MediaFileModel;
import com.chronos.chronos.model.UserModel;
import com.chronos.chronos.repositiory.CapsuleRepo;
import com.chronos.chronos.repositiory.CapsuleShareRepo;
import com.chronos.chronos.repositiory.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CapsuleService {
    private static final int MAX_SHARE_RECIPIENTS = 50;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final CapsuleRepo capsuleRepo;
    private final CapsuleShareRepo capsuleShareRepo;
    private final UserRepo userRepo;
    private final FileStorageService fileStorageService;
    private final MailService mailService;

    @Transactional
    public CapsuleResponse create(String email, CapsuleRequest request) {

        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Capsule payload is required.", "CAPSULE_PAYLOAD_REQUIRED");
        }

        String normalizedOwnerEmail = normalizeEmail(email);
        UserModel user = userRepo.findByEmail(normalizedOwnerEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found.", "USER_NOT_FOUND"));

        if (!StringUtils.hasText(request.getTitle())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Capsule title is required.", "CAPSULE_TITLE_REQUIRED");
        }
        if (request.getUnlockAt() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Capsule unlock date is required.",
                    "CAPSULE_UNLOCK_DATE_REQUIRED");
        }

        int mediaCount = safeSize(request.getPhotos()) + safeSize(request.getVideos());
        if (Boolean.TRUE.equals(request.getRequireMedia()) && mediaCount == 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Media upload is required for this capsule. Please upload media and try again.",
                    "MEDIA_REQUIRED");
        }

        List<CapsuleRequest.ShareRecipient> recipients = resolveRecipients(request);

        validateMediaReferences(request.getPhotos(), "photo");
        validateMediaReferences(request.getVideos(), "video");

        CapsuleModel capsule = new CapsuleModel();
        capsule.setUser(user);
        capsule.setTitle(request.getTitle().trim());
        capsule.setContent(request.getContent());
        capsule.setUnlockDate(request.getUnlockAt());
        capsule.setShareEmail(recipients.isEmpty() ? null : recipients.get(0).getEmail());
        capsule.setWeather(request.getWeather());
        capsule.setLocked(request.getUnlockAt().isAfter(LocalDateTime.now()));

        if (request.getPhotos() != null) {
            request.getPhotos().forEach(photo -> {
                MediaFileModel media = new MediaFileModel();
                media.setName(photo.getName());
                media.setUrl(photo.getUrl());
                media.setType(photo.getType());
                media.setMediaType("photo");
                media.setCapsule(capsule);
                capsule.getPhotos().add(media);
            });
        }
        if (request.getVideos() != null) {
            request.getVideos().forEach(video -> {
                MediaFileModel media = new MediaFileModel();
                media.setName(video.getName());
                media.setUrl(video.getUrl());
                media.setType(video.getType());
                media.setMediaType("video");
                media.setCapsule(capsule);
                capsule.getVideos().add(media);
            });
        }

        CapsuleModel savedCapsule = capsuleRepo.save(capsule);
        createShareRecords(savedCapsule, recipients, normalizedOwnerEmail, user.getName());

        return toResponse(savedCapsule, "OWNER", null, null);

    }

    private void createShareRecords(CapsuleModel capsule,
            List<CapsuleRequest.ShareRecipient> recipients,
            String ownerEmail,
            String ownerName) {
        if (recipients.isEmpty()) {
            return;
        }

        for (CapsuleRequest.ShareRecipient recipient : recipients) {
            String recipientEmail = normalizeAndValidateRecipientEmail(recipient.getEmail(), ownerEmail);

            CapsuleShareModel share = new CapsuleShareModel();
            share.setCapsule(capsule);
            share.setRecipientEmail(recipientEmail);
            share.setCanReshare(Boolean.TRUE.equals(recipient.getCanReshare()));
            share.setStatus(CapsuleShareStatus.PENDING);
            capsuleShareRepo.save(share);

            try {
                mailService.Share(recipientEmail, ownerName);
            } catch (Exception ignored) {
                // Notification failures should not fail capsule creation.
            }
        }
    }

    private List<CapsuleRequest.ShareRecipient> resolveRecipients(CapsuleRequest request) {
        List<CapsuleRequest.ShareRecipient> merged = new ArrayList<>();
        if (request.getRecipients() != null) {
            merged.addAll(request.getRecipients());
        }

        if (StringUtils.hasText(request.getShareEmail())) {
            merged.add(new CapsuleRequest.ShareRecipient(request.getShareEmail(), false));
        }

        if (merged.size() > MAX_SHARE_RECIPIENTS) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "You can share a capsule with up to " + MAX_SHARE_RECIPIENTS + " recipients.",
                    "SHARE_LIMIT_EXCEEDED");
        }

        Set<String> seen = new HashSet<>();
        List<CapsuleRequest.ShareRecipient> normalized = new ArrayList<>();
        for (CapsuleRequest.ShareRecipient recipient : merged) {
            if (recipient == null || !StringUtils.hasText(recipient.getEmail())) {
                continue;
            }

            String normalizedEmail = normalizeEmail(recipient.getEmail());
            if (!isValidEmail(normalizedEmail)) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "One or more share email addresses are invalid.",
                        "SHARE_EMAIL_INVALID");
            }
            if (!seen.add(normalizedEmail)) {
                // If it's a duplicate, just skip it instead of throwing an error.
                // This gracefully handles cases where frontend sends the same email in both
                // `recipients[]` and `shareEmail`.
                continue;
            }

            normalized.add(
                    new CapsuleRequest.ShareRecipient(normalizedEmail, Boolean.TRUE.equals(recipient.getCanReshare())));
        }

        return normalized;
    }

    private String normalizeAndValidateRecipientEmail(String rawEmail, String ownerEmail) {
        String normalized = normalizeEmail(rawEmail);

        if (!isValidEmail(normalized)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "One or more share email addresses are invalid.",
                    "SHARE_EMAIL_INVALID");
        }
        if (normalized.equals(ownerEmail)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Owner email cannot be added as a share recipient.",
                    "SHARE_SELF_NOT_ALLOWED");
        }

        // Ensure recipient exists in the users table before allowing share
        if (!userRepo.existsByEmail(normalized)) {
            throw new ApiException(HttpStatus.NOT_FOUND,
                    "Recipient email '" + normalized + "' is not registered. Please ensure they have an account.",
                    "RECIPIENT_NOT_REGISTERED");
        }

        return normalized;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isValidEmail(String email) {
        return StringUtils.hasText(email) && EMAIL_PATTERN.matcher(email).matches();
    }

    private void validateMediaReferences(List<CapsuleRequest.MediaRef> mediaRefs, String mediaType) {
        if (mediaRefs == null) {
            return;
        }

        for (CapsuleRequest.MediaRef mediaRef : mediaRefs) {
            if (mediaRef == null || !StringUtils.hasText(mediaRef.getUrl())) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid " + mediaType + " reference in request.",
                        "INVALID_MEDIA_REFERENCE");
            }

            if (!fileStorageService.mediaFileExists(mediaRef.getUrl())) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "One or more uploaded media files are missing. Please upload again before creating the capsule.",
                        "MEDIA_FILE_NOT_FOUND");
            }
        }
    }

    private int safeSize(List<?> list) {
        return list == null ? 0 : list.size();
    }

    public List<CapsuleResponse> getCapsules(String email) {
        return capsuleRepo.findByUserEmailOrderByCreatedAtDesc(normalizeEmail(email))
                .stream().map(c -> toResponse(c, "OWNER", null, null)).toList();
    }

    @Transactional
    public List<CapsuleResponse> getDashboardCapsules(String email, String scope) {
        String normalizedEmail = normalizeEmail(email);
        DashboardScope dashboardScope = DashboardScope.from(scope);

        List<CapsuleResponse> feed = new ArrayList<>();
        if (dashboardScope.includeOwned()) {
            List<CapsuleResponse> owned = capsuleRepo.findByUserEmailOrderByCreatedAtDesc(normalizedEmail)
                    .stream()
                    .map(c -> toResponse(c, "OWNER", null, null))
                    .toList();
            feed.addAll(owned);
        }

        if (dashboardScope.includeShared()) {
            Collection<CapsuleShareStatus> visibleStatuses = List.of(CapsuleShareStatus.PENDING,
                    CapsuleShareStatus.ACCEPTED);
            List<CapsuleResponse> shared = capsuleShareRepo
                    .findByRecipientEmailAndStatusInOrderByCreatedAtDesc(normalizedEmail, visibleStatuses)
                    .stream()
                    .map(share -> toResponse(
                            share.getCapsule(),
                            "SHARED",
                            share.getStatus().name(),
                            share.getCapsule().getUser() == null ? null : share.getCapsule().getUser().getEmail()))
                    .toList();
            feed.addAll(shared);
        }

        feed.sort(Comparator
                .comparing(CapsuleResponse::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(CapsuleResponse::getId, Comparator.nullsLast(Comparator.reverseOrder())));
        return feed;
    }

    private CapsuleResponse toResponse(CapsuleModel c, String accessType, String shareStatus, String sharedByEmail) {
        CapsuleResponse r = new CapsuleResponse();
        LocalDateTime now = LocalDateTime.now();
        boolean locked = c.getUnlockDate() != null && c.getUnlockDate().isAfter(now);

        r.setId(c.getId());
        r.setTitle(c.getTitle());
        r.setContent(locked ? null : c.getContent());
        r.setShareEmail(c.getShareEmail());
        r.setSharedWith(c.getShares().stream().map(CapsuleShareModel::getRecipientEmail).toList());
        r.setWeather(c.getWeather());
        r.setCreatedAt(c.getCreatedAt());
        r.setUnlockAt(c.getUnlockDate());
        r.setLocked(locked);
        r.setCanViewContent(!locked);
        r.setCanManageShares("OWNER".equals(accessType));
        r.setAccessType(accessType);
        r.setShareStatus(shareStatus);
        r.setSharedByEmail(sharedByEmail);
        r.setPhotos((locked ? List.<MediaFileModel>of() : c.getPhotos()).stream().map(m -> {
            var d = new CapsuleResponse.MediaDto();
            d.setName(m.getName());
            d.setType(m.getType());
            d.setUrl(m.getUrl());
            return d;
        }).toList());
        r.setVideos((locked ? List.<MediaFileModel>of() : c.getVideos()).stream().map(m -> {
            var d = new CapsuleResponse.MediaDto();
            d.setName(m.getName());
            d.setType(m.getType());
            d.setUrl(m.getUrl());
            return d;
        }).toList());
        return r;
    }

    private enum DashboardScope {
        OWNED,
        SHARED,
        ALL;

        static DashboardScope from(String value) {
            if (!StringUtils.hasText(value)) {
                return ALL;
            }

            try {
                return DashboardScope.valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Scope must be one of: owned, shared, all.",
                        "INVALID_DASHBOARD_SCOPE");
            }
        }

        boolean includeOwned() {
            return this == OWNED || this == ALL;
        }

        boolean includeShared() {
            return this == SHARED || this == ALL;
        }
    }
}
