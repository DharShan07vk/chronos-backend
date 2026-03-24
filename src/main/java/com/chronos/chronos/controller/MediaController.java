package com.chronos.chronos.controller;

import com.chronos.chronos.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
public class MediaController {

    private final FileStorageService fileStorageService;

    @Value("${app.media.cache-max-age-seconds:31536000}")
    private long cacheMaxAgeSeconds;

    @GetMapping({ "/media/{filename:.+}", "/uploads/{filename:.+}" })
    public ResponseEntity<Resource> getMedia(
            @PathVariable String filename,
            @RequestHeader(value = "Range", required = false) String rangeHeader) throws IOException {

        Path filePath = fileStorageService.resolve(filename);
        if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
            return ResponseEntity.notFound().build();
        }

        long fileLength = Files.size(filePath);
        long lastModifiedMillis = Files.getLastModifiedTime(filePath).toMillis();
        String eTag = "\"" + fileLength + "-" + lastModifiedMillis + "\"";
        MediaType mediaType = fileStorageService.detectMediaType(filePath);

        if (rangeHeader == null || rangeHeader.isBlank()) {
            Resource resource = new UrlResource(filePath.toUri());
            return ResponseEntity.ok()
                    .headers(commonHeaders(fileLength, eTag, lastModifiedMillis))
                    .contentType(mediaType)
                    .contentLength(fileLength)
                    .body(resource);
        }

        long[] range = parseRange(rangeHeader, fileLength);
        if (range == null) {
            HttpHeaders headers = commonHeaders(fileLength, eTag, lastModifiedMillis);
            headers.set(HttpHeaders.CONTENT_RANGE, "bytes */" + fileLength);
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                    .headers(headers)
                    .build();
        }

        long start = range[0];
        long end = range[1];
        int rangeLength = (int) (end - start + 1);

        byte[] data = readRange(filePath, start, rangeLength);
        Resource resource = new ByteArrayResource(data);

        HttpHeaders headers = commonHeaders(fileLength, eTag, lastModifiedMillis);
        headers.set(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileLength);

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .headers(headers)
                .contentType(mediaType)
                .contentLength(rangeLength)
                .body(resource);
    }

    private HttpHeaders commonHeaders(long fileLength, String eTag, long lastModifiedMillis) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        headers.setETag(eTag);
        headers.setLastModified(lastModifiedMillis);
        headers.setCacheControl(CacheControl.maxAge(cacheMaxAgeSeconds, TimeUnit.SECONDS).cachePublic().immutable());
        return headers;
    }

    private long[] parseRange(String rangeHeader, long fileLength) {
        if (!rangeHeader.startsWith("bytes=")) {
            return null;
        }

        String rangeValue = rangeHeader.substring("bytes=".length()).trim();
        if (rangeValue.contains(",")) {
            return null;
        }

        String[] parts = rangeValue.split("-", 2);
        try {
            long start;
            long end;

            if (parts[0].isBlank()) {
                long suffixLength = Long.parseLong(parts[1]);
                if (suffixLength <= 0) {
                    return null;
                }
                suffixLength = Math.min(suffixLength, fileLength);
                start = fileLength - suffixLength;
                end = fileLength - 1;
            } else {
                start = Long.parseLong(parts[0]);
                if (start < 0 || start >= fileLength) {
                    return null;
                }

                if (parts.length == 1 || parts[1].isBlank()) {
                    end = fileLength - 1;
                } else {
                    end = Long.parseLong(parts[1]);
                    if (end < start) {
                        return null;
                    }
                    end = Math.min(end, fileLength - 1);
                }
            }

            return new long[] { start, end };
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private byte[] readRange(Path filePath, long start, int length) throws IOException {
        byte[] data = new byte[length];
        try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "r")) {
            raf.seek(start);
            raf.readFully(data);
        }
        return data;
    }
}
