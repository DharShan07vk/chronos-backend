package com.chronos.chronos.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLRestriction;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "capsules")
@Data
public class CapsuleModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String content;

    private String shareEmail;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime unlockDate;

    private boolean locked;

    private String weather;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserModel user;

    @OneToMany(mappedBy = "capsule", cascade = CascadeType.ALL, orphanRemoval = true)
    @SQLRestriction("media_type = 'photo'")
    private List<MediaFileModel> photos = new ArrayList<>();

    @OneToMany(mappedBy = "capsule", cascade = CascadeType.ALL, orphanRemoval = true)
    @SQLRestriction("media_type = 'video'")
    private List<MediaFileModel> videos = new ArrayList<>();
}
//
// id: string;
// title: string;
// content: string;
// createdAt: Date;
// unlockAt: Date;
// shareEmail?: string;
// weather?: string;
// isLocked: boolean;
// photos: MediaFile[];
// videos: MediaFile[];
