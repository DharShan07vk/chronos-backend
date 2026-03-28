package com.chronos.chronos.controller;

import com.chronos.chronos.dto.ApiResponse;
import com.chronos.chronos.dto.CapsuleRequest;
import com.chronos.chronos.dto.CapsuleResponse;
import com.chronos.chronos.exception.ApiException;
import com.chronos.chronos.service.CapsuleService;
import com.chronos.chronos.service.FileStorageService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("capsules/")
@RequiredArgsConstructor
public class CapsuleController {
    private final CapsuleService capsuleService;
    private final FileStorageService fileStorageService;

    @PostMapping("new")
    public ResponseEntity<ApiResponse<CapsuleResponse>> createCapsule(@RequestBody CapsuleRequest request,
            @AuthenticationPrincipal UserDetails user) {
        CapsuleResponse response = capsuleService.create(user.getUsername(), request);
        System.out.println(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Capsule Created", response));
    }

    @PostMapping("upload")
    public ResponseEntity<List<Map<String, String>>> uploadFiles(
            @RequestParam("files") List<MultipartFile> files) throws Exception {
        if (CollectionUtils.isEmpty(files)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "At least one file is required.", "FILES_REQUIRED");
        }

        List<Map<String, String>> result = new ArrayList<>();
        for (MultipartFile file : files) {
            String filename = fileStorageService.store(file);
            String mediaUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/media/")
                    .path(filename)
                    .build()
                    .toUriString();

            result.add(Map.of(
                    "name", file.getOriginalFilename(),
                    "type", file.getContentType(),
                    "url", mediaUrl));
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<List<CapsuleResponse>> getAllCapsules(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(capsuleService.getCapsules(user.getUsername()));
    }

    @GetMapping("dashboard")
    public ResponseEntity<List<CapsuleResponse>> getDashboard(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(name = "scope", required = false, defaultValue = "all") String scope) {
        return ResponseEntity.ok(capsuleService.getDashboardCapsules(user.getUsername(), scope));
    }

}
