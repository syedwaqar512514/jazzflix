package org.jazz.jazzflix.service.video;

import org.jazz.jazzflix.dto.VideoUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface VideoUploadService {
    VideoUploadResponse handleUpload(MultipartFile file);
}
