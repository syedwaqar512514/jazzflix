package org.jazz.jazzflix.service.video;

import org.jazz.jazzflix.dto.VideoUploadResponse;
import org.jazz.jazzflix.dto.video.VideoAssetDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface VideoUploadService {
    VideoUploadResponse handleUpload(MultipartFile file, UUID userId);
    List<VideoAssetDto> getAllVideosByUserId(UUID userId);
}
