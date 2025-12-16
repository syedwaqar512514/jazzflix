package org.jazz.jazzflix.dto.video;

import java.time.OffsetDateTime;
import java.util.UUID;

public class VideoUploadEvent {
    private UUID id;
    private String objectKey;
    private String bucket;
    private long sizeBytes;
    private String contentType;
    private OffsetDateTime uploadedAt;

    public VideoUploadEvent(UUID id, String objectKey, String bucket, long sizeBytes, String contentType, OffsetDateTime uploadedAt) {
        this.id = id;
        this.objectKey = objectKey;
        this.bucket = bucket;
        this.sizeBytes = sizeBytes;
        this.contentType = contentType;
        this.uploadedAt = uploadedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getBucket() {
        return bucket;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getContentType() {
        return contentType;
    }

    public OffsetDateTime getUploadedAt() {
        return uploadedAt;
    }
}
