package com.chronos.chronos.service;

import java.util.List;
import com.chronos.chronos.dto.CapsuleRequest;
import com.chronos.chronos.dto.CapsuleResponse;
import com.chronos.chronos.model.CapsuleModel;
import com.chronos.chronos.model.MediaFileModel;
import com.chronos.chronos.model.UserModel;
import com.chronos.chronos.repositiory.CapsuleRepo;
import com.chronos.chronos.repositiory.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CapsuleService {
    private final CapsuleRepo capsuleRepo;
    private final UserRepo userRepo;

    public CapsuleResponse create(String email, CapsuleRequest request) {
        UserModel user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User with email " + email + " not found"));
        CapsuleModel capsule = new CapsuleModel();
        capsule.setUser(user);
        capsule.setTitle(request.getTitle());
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
