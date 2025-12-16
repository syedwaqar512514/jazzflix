package org.jazz.jazzflix.entity.video;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "TBL_VIDEO_QUALITY")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TblVideoQuality {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "video_id", nullable = false)
    private UUID videoId;

    @Column(name = "quality", nullable = false, length = 20)
    private String quality; // "ORIGINAL", "1080p", "720p", "480p", "360p"

    @Column(name = "resolution", nullable = false, length = 20)
    private String resolution; // "1920x1080", "1280x720", etc.

    @Column(name = "bitrate", length = 20)
    private String bitrate; // "5000k", "3000k", etc.

    @Column(name = "object_key", nullable = false, length = 255)
    private String objectKey;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "content_type", length = 150)
    private String contentType;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // "PENDING", "PROCESSING", "COMPLETED", "FAILED"

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Timestamp createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Timestamp.from(Instant.now());
        }
        if (status == null) {
            status = "PENDING";
        }
    }
}