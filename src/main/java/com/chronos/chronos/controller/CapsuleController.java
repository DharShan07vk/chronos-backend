package com.chronos.chronos.controller;

import com.chronos.chronos.dto.ApiResponse;
import com.chronos.chronos.dto.CapsuleRequest;
import com.chronos.chronos.dto.CapsuleResponse;
import com.chronos.chronos.model.UserModel;
import com.chronos.chronos.repositiory.CapsuleRepo;
import com.chronos.chronos.service.CapsuleService;
import com.chronos.chronos.service.FileStorageService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
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

        try {
            CapsuleResponse response = capsuleService.create(user.getUsername(), request);
            return ResponseEntity.ok(new ApiResponse<>(true, "Capsule Created", response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }

    }

    @PostMapping("upload")
    public ResponseEntity<List<Map<String, String>>> uploadFiles(
            @RequestParam("files") List<MultipartFile> files) throws Exception {
        List<Map<String, String>> result = new ArrayList<>();
        for (MultipartFile file : files) {
            String url = fileStorageService.store(file);
            result.add(Map.of(
                    "name", file.getOriginalFilename(),
                    "type", file.getContentType(),
                    "url", "https://chronos-backend-5ovw.onrender.com" + url));
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<List<CapsuleResponse>> getAllCapsules(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(capsuleService.getCapsules(user.getUsername()));
    }

}
