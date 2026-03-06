package com.chronos.chronos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CapsuleResponse {
    private Long id;
    private String title;
    private String content;
    private String shareEmail;
    private String weather;
    private LocalDateTime createdAt;
    private LocalDateTime unlockAt;
    private boolean isLocked;
    private List<MediaDto> photos;
    private List<MediaDto> videos;

    @Data
    public static class MediaDto {
        private String name;
        private String type;
        private String url;
    }
}
