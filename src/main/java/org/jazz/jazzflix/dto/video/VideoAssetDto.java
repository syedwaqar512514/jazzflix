package org.jazz.jazzflix.dto.video;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VideoAssetDto {
    public UUID id;
    public UUID ownerId;
    public String thumbnail;
    public String manifestPath;
    public int sizeBytes;
    public String bucket;
}
