package com.chronos.chronos.service;

import java.util.List;
import com.chronos.chronos.dto.CapsuleRequest;
import com.chronos.chronos.dto.CapsuleResponse;
import com.chronos.chronos.exception.ApiException;
import com.chronos.chronos.model.CapsuleModel;
import com.chronos.chronos.model.MediaFileModel;
import com.chronos.chronos.model.UserModel;
import com.chronos.chronos.repositiory.CapsuleRepo;
import com.chronos.chronos.repositiory.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CapsuleService {
    private final CapsuleRepo capsuleRepo;
    private final UserRepo userRepo;
    private final FileStorageService fileStorageService;

    public CapsuleResponse create(String email, CapsuleRequest request) {
        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Capsule payload is required.", "CAPSULE_PAYLOAD_REQUIRED");
        }

        UserModel user = userRepo.findByEmail(email)
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

        validateMediaReferences(request.getPhotos(), "photo");
        validateMediaReferences(request.getVideos(), "video");

        CapsuleModel capsule = new CapsuleModel();
        capsule.setUser(user);
        capsule.setTitle(request.getTitle().trim());
        capsule.setContent(request.getContent());
        capsule.setUnlockDate(request.getUnlockAt());
        capsule.setShareEmail(request.getShareEmail());
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

        return toResponse(capsuleRepo.save(capsule));

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
        return capsuleRepo.findByUserEmailOrderByCreatedAtDesc(email)
                .stream().map(this::toResponse).toList();
    }

    private CapsuleResponse toResponse(CapsuleModel c) {
        CapsuleResponse r = new CapsuleResponse();
        r.setId(c.getId());
        r.setTitle(c.getTitle());
        r.setContent(c.getContent());
        r.setShareEmail(c.getShareEmail());
        r.setWeather(c.getWeather());
        r.setCreatedAt(c.getCreatedAt());
        r.setUnlockAt(c.getUnlockDate());
        r.setLocked(c.getUnlockDate() != null && c.getUnlockDate().isAfter(LocalDateTime.now()));
        r.setPhotos(c.getPhotos().stream().map(m -> {
            var d = new CapsuleResponse.MediaDto();
            d.setName(m.getName());
            d.setType(m.getType());
            d.setUrl(m.getUrl());
            return d;
        }).toList());
        r.setVideos(c.getVideos().stream().map(m -> {
            var d = new CapsuleResponse.MediaDto();
            d.setName(m.getName());
            d.setType(m.getType());
            d.setUrl(m.getUrl());
            return d;
        }).toList());
        return r;
    }
}
