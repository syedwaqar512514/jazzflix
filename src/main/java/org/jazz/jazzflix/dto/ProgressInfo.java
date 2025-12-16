package org.jazz.jazzflix.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgressInfo {
    private String uploadId;
    private String fileName;
    private long totalBytes;
    private long uploadedBytes;
    private int progressPercentage;
    private String status; // "UPLOADING", "PROCESSING", "THUMBNAIL", "COMPLETED", "FAILED"
    private String message;
    private LocalDateTime startTime;
    private LocalDateTime lastUpdateTime;
    private String videoId; // Set when upload completes

    public ProgressInfo(String uploadId, String fileName, long totalBytes) {
        this.uploadId = uploadId;
        this.fileName = fileName;
        this.totalBytes = totalBytes;
        this.uploadedBytes = 0;
        this.progressPercentage = 0;
        this.status = "UPLOADING";
        this.message = "Upload started";
        this.startTime = LocalDateTime.now();
        this.lastUpdateTime = LocalDateTime.now();
    }

    public void updateProgress(long bytesUploaded) {
        this.uploadedBytes = bytesUploaded;
        this.progressPercentage = totalBytes > 0 ? (int) ((bytesUploaded * 100) / totalBytes) : 0;
        this.lastUpdateTime = LocalDateTime.now();

        if (progressPercentage >= 100) {
            this.status = "PROCESSING";
            this.message = "Upload completed, processing video...";
        } else {
            this.message = String.format("Uploading... %d%% (%d/%d bytes)",
                progressPercentage, uploadedBytes, totalBytes);
        }
    }

    public void setStatus(String status, String message) {
        this.status = status;
        this.message = message;
        this.lastUpdateTime = LocalDateTime.now();
    }

    public void complete(String videoId) {
        this.status = "COMPLETED";
        this.message = "Video upload completed successfully";
        this.progressPercentage = 100;
        this.videoId = videoId;
        this.lastUpdateTime = LocalDateTime.now();
    }

    public void fail(String errorMessage) {
        this.status = "FAILED";
        this.message = "Upload failed: " + errorMessage;
        this.lastUpdateTime = LocalDateTime.now();
    }
}