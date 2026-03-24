package com.chronos.chronos.service;

import com.chronos.chronos.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final long MAX_FILE_SIZE_BYTES = 50L * 1024 * 1024;
    private final Path uploadDir = Paths.get("uploads");

    public FileStorageService() throws IOException {
        Files.createDirectories(uploadDir);
    }

    public String store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Empty file upload is not allowed.", "EMPTY_FILE_UPLOAD");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ApiException(
                    HttpStatusCode.valueOf(413),
                    "File size exceeds the 50MB limit. Please choose a smaller file.",
                    "FILE_SIZE_LIMIT_EXCEEDED");
        }

        String originalName = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String cleanName = StringUtils.cleanPath(Path.of(originalName).getFileName().toString());
        String filename = UUID.randomUUID() + "_" + cleanName;
        Files.copy(file.getInputStream(), uploadDir.resolve(filename),
                StandardCopyOption.REPLACE_EXISTING);
        return filename;
    }

    public boolean mediaFileExists(String mediaUrl) {
        String filename = extractFilenameFromMediaUrl(mediaUrl);
        if (!StringUtils.hasText(filename)) {
            return false;
        }

        try {
            Path resolved = resolve(filename);
            return Files.exists(resolved) && Files.isReadable(resolved);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public Path resolve(String filename) {
        Path normalized = uploadDir.resolve(filename).normalize();
        if (!normalized.startsWith(uploadDir)) {
            throw new IllegalArgumentException("Invalid file path");
        }
        return normalized;
    }

    public MediaType detectMediaType(Path path) {
        try {
            String contentType = Files.probeContentType(path);
            if (contentType != null && !contentType.isBlank()) {
                return MediaType.parseMediaType(contentType);
            }
        } catch (IOException ignored) {
            // Fall through to a generic binary type when type probing fails.
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private String extractFilenameFromMediaUrl(String mediaUrl) {
        if (!StringUtils.hasText(mediaUrl)) {
            return null;
        }

        String trimmed = mediaUrl.trim();
        String path;
        try {
            URI uri = URI.create(trimmed);
            path = uri.getPath();
        } catch (IllegalArgumentException ex) {
            path = trimmed;
        }

        if (!StringUtils.hasText(path)) {
            return null;
        }

        int queryIdx = path.indexOf('?');
        String cleanPath = queryIdx >= 0 ? path.substring(0, queryIdx) : path;
        int lastSlash = cleanPath.lastIndexOf('/');
        String rawFilename = lastSlash >= 0 ? cleanPath.substring(lastSlash + 1) : cleanPath;

        if (!StringUtils.hasText(rawFilename)) {
            return null;
        }

        return URLDecoder.decode(rawFilename, StandardCharsets.UTF_8);
    }
}