package com.chronos.chronos.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "capsule_shares", uniqueConstraints = {
                @UniqueConstraint(name = "uk_capsule_recipient", columnNames = { "capsule_id", "recipient_email" })
}, indexes = {
                @Index(name = "idx_share_recipient_status_created", columnList = "recipient_email,status,created_at"),
                @Index(name = "idx_share_capsule", columnList = "capsule_id")
})
@Data
public class CapsuleShareModel {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "capsule_id", nullable = false)
        private CapsuleModel capsule;

        @Column(name = "recipient_email", nullable = false)
        private String recipientEmail;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        private CapsuleShareStatus status = CapsuleShareStatus.PENDING;

        @Column(name = "can_reshare", nullable = false)
        private boolean canReshare = false;

        @Column(name = "created_at", nullable = false)
        private LocalDateTime createdAt = LocalDateTime.now();

        @Column(name = "updated_at")
        private LocalDateTime updatedAt;

        @Column(name = "accepted_at")
        private LocalDateTime acceptedAt;

        @Column(name = "revoked_at")
        private LocalDateTime revokedAt;

        @PreUpdate
        public void onUpdate() {
                this.updatedAt = LocalDateTime.now();
        }
}
