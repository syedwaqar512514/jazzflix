package org.jazz.jazzflix.controller.video;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import org.jazz.jazzflix.config.storage.MinioProperties;
import org.jazz.jazzflix.dto.ProgressInfo;
import org.jazz.jazzflix.dto.Response;
import org.jazz.jazzflix.dto.VideoUploadResponse;
import org.jazz.jazzflix.entity.video.TblVideoAssest;
import org.jazz.jazzflix.exception.DataNotFoundException;
import org.jazz.jazzflix.repository.video.TblVideoAssestRepository;
import org.jazz.jazzflix.service.ProgressService;
import org.jazz.jazzflix.service.VideoTranscodingService;
import org.jazz.jazzflix.service.video.VideoUploadService;
import org.jazz.jazzflix.dto.VideoQualityDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/video")
public class VideoUploadController {
    private final VideoUploadService videoUploadService;
    private final TblVideoAssestRepository videoAssestRepository;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final ProgressService progressService;
    private final VideoTranscodingService transcodingService;

    public VideoUploadController(VideoUploadService videoUploadService,
                                 TblVideoAssestRepository videoAssestRepository,
                                 MinioClient minioClient,
                                 MinioProperties minioProperties,
                                 ProgressService progressService,
                                 VideoTranscodingService transcodingService) {
        this.videoUploadService = videoUploadService;
        this.videoAssestRepository = videoAssestRepository;
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
        this.progressService = progressService;
        this.transcodingService = transcodingService;
    }

    @PostMapping("/api/upload")
    public ResponseEntity<Response<VideoUploadResponse>> uploadVideo(@RequestParam("file") MultipartFile file,
                                                            HttpServletRequest httpRequest) {
        VideoUploadResponse uploadResponse = videoUploadService.handleUpload(file);
        boolean success = true;
        String message = "Video upload initiated. Use the uploadId to track progress.";
        int status = HttpStatus.OK.value();
        String path = httpRequest.getRequestURI();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        Response<VideoUploadResponse> response = new Response<>(success, message, uploadResponse, status, path, timestamp);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping(value = "/api/thumbnail/{videoId}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getThumbnail(@PathVariable UUID videoId) {
        try {
            TblVideoAssest video = videoAssestRepository.findById(videoId)
                    .orElseThrow(() -> new DataNotFoundException("Video not found with ID: " + videoId));

            if (video.getThumbnailObjectKey() == null) {
                return ResponseEntity.notFound().build();
            }

            GetObjectArgs getArgs = GetObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(video.getThumbnailObjectKey())
                    .build();

            byte[] thumbnailData = minioClient.getObject(getArgs).readAllBytes();
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(thumbnailData);

        } catch (DataNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/progress/{uploadId}")
    public ResponseEntity<ProgressInfo> getUploadProgress(@PathVariable String uploadId) {
        ProgressInfo progress = progressService.getProgress(uploadId);
        if (progress == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(progress);
    }

    @GetMapping("/api/qualities/{videoId}")
    public ResponseEntity<Response<List<VideoQualityDto>>> getVideoQualities(@PathVariable UUID videoId) {
        try {
            List<VideoQualityDto> qualities = transcodingService.getVideoQualities(videoId);
            boolean success = true;
            String message = "Video qualities retrieved successfully.";
            int status = HttpStatus.OK.value();
            String path = "/video/api/qualities/" + videoId;
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
            Response<List<VideoQualityDto>> response = new Response<>(success, message, qualities, status, path, timestamp);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            boolean success = false;
            String message = "Failed to retrieve video qualities: " + e.getMessage();
            int status = HttpStatus.INTERNAL_SERVER_ERROR.value();
            String path = "/video/api/qualities/" + videoId;
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
            Response<List<VideoQualityDto>> response = new Response<>(success, message, null, status, path, timestamp);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/api/download/{videoId}/{quality}")
    public ResponseEntity<byte[]> downloadVideo(@PathVariable UUID videoId, @PathVariable String quality) {
        try {
            // Find the video quality record
            List<VideoQualityDto> qualities = transcodingService.getVideoQualities(videoId);
            VideoQualityDto selectedQuality = qualities.stream()
                    .filter(q -> q.getQuality().equalsIgnoreCase(quality))
                    .findFirst()
                    .orElseThrow(() -> new DataNotFoundException("Quality not found: " + quality));

            if (!"COMPLETED".equals(selectedQuality.getStatus())) {
                return ResponseEntity.status(HttpStatus.PROCESSING)
                        .header("X-Processing-Status", selectedQuality.getStatus())
                        .build();
            }

            // Get the object key for this quality
            String objectKey = "videos/qualities/" + quality.toLowerCase() + "/" +
                    videoId + "_" + quality.toLowerCase() + ".mp4";

            GetObjectArgs getArgs = GetObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(objectKey)
                    .build();

            byte[] videoData = minioClient.getObject(getArgs).readAllBytes();

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("video/mp4"))
                    .header("Content-Disposition", "attachment; filename=\"" + videoId + "_" + quality + ".mp4\"")
                    .body(videoData);

        } catch (DataNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/dash/{videoId}/manifest.mpd")
    public ResponseEntity<byte[]> getDashManifest(@PathVariable UUID videoId) {
        try {
            String objectKey = "videos/" + videoId + "/dash/manifest.mpd";

            GetObjectArgs getArgs = GetObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(objectKey)
                    .build();

            byte[] manifestData = minioClient.getObject(getArgs).readAllBytes();

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/dash+xml"))
                    .body(manifestData);

        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/api/dash/{videoId}/**")
    public ResponseEntity<byte[]> getDashSegment(@PathVariable UUID videoId, HttpServletRequest request) {
        try {
            // Extract the segment path from the request URI
            String requestUri = request.getRequestURI();
            String segmentPath = requestUri.substring(requestUri.indexOf("/api/dash/" + videoId + "/") + ("/api/dash/" + videoId + "/").length());

            String objectKey = "videos/" + videoId + "/dash/" + segmentPath;

            GetObjectArgs getArgs = GetObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(objectKey)
                    .build();

            byte[] segmentData = minioClient.getObject(getArgs).readAllBytes();

            String contentType = segmentPath.endsWith(".m4s") ? "video/iso.segment" : "application/octet-stream";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(segmentData);

        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
