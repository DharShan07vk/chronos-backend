package com.chronos.chronos.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "media_files")
@Data
public class MediaFileModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String type;
    private String url;
    private String mediaType; // "photo" or "video"

    @ManyToOne
    @JoinColumn(name = "capsule_id")
    private CapsuleModel capsule;
}
