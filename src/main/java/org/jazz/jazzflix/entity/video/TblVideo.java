//package org.jazz.jazzflix.entity.video;
//
//import jakarta.persistence.*;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//import java.sql.Timestamp;
//import java.util.UUID;
//
//@Entity
//@Table(name = "TBL_VIDEO")
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//public class TblVideo {
//
//    @Id
//    @Column(name = "id", nullable = false, updatable = false)
//    private UUID id;
//
//    @Column(name = "original_file_name", nullable = false, length = 255)
//    private String originalFileName;
//
//    @Column(name = "object_key", nullable = false, length = 255, unique = true)
//    private String objectKey;
//
//    @Column(name = "content_type", length = 150)
//    private String contentType;
//
//    @Column(name = "size_bytes", nullable = false)
//    private long sizeBytes;
//
//    @Column(name = "status", nullable = false, length = 50)
//    private String status;
//
//    @Column(name = "bucket", nullable = false, length = 120)
//    private String bucket;
//
//    @Column(name = "thumbnail_object_key", length = 255)
//    private String thumbnailObjectKey;
//
//    @Column(name = "preferred_quality", nullable = false, length = 20)
//    private String preferredQuality; // "1080p", "720p", "480p", "360p"
//
//    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
//    private Timestamp createdAt;
//
//    @PrePersist
//    public void prePersist() {
//        if (id == null) {
//            id = UUID.randomUUID();
//        }
//        if (createdAt == null) {
//            createdAt = Timestamp.from(java.time.Instant.now());
//        }
//        if (preferredQuality == null) {
//            preferredQuality = "720p";
//        }
//    }
//}