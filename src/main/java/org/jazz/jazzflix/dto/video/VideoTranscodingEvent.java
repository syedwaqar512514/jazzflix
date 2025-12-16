package org.jazz.jazzflix.dto.video;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoTranscodingEvent {
    private UUID videoId;
    private String originalObjectKey;
    private String contentType;
    private String quality;

    public String getOriginalObjectKey() {
        return originalObjectKey;
    }

    public String getContentType() {
        return contentType;
    }

    public String getQuality() {
        return quality;
    }
}