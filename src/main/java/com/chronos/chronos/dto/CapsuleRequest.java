package com.chronos.chronos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CapsuleRequest {
    private String title;
    private String content;
    private LocalDateTime unlockAt;
    private String shareEmail;
    private List<ShareRecipient> recipients;
    private String weather;
    private Boolean requireMedia;
    private List<MediaRef> photos;
    private List<MediaRef> videos;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShareRecipient {
        private String email;
        private Boolean canReshare;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MediaRef {
        private String url;
        private String type;
        private String name;
    }

}
