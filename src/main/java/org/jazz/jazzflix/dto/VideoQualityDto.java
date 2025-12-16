package org.jazz.jazzflix.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoQualityDto {
    private String quality; // "ORIGINAL", "1080p", "720p", "480p", "360p"
    private String resolution; // "1920x1080", "1280x720", etc.
    private String bitrate; // "5000k", "3000k", etc.
    private long sizeBytes;
    private String status; // "PENDING", "PROCESSING", "COMPLETED", "FAILED"
    private LocalDateTime createdAt;
    private String downloadUrl;
}