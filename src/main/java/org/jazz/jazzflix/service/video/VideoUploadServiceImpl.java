package org.jazz.jazzflix.service.video;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.jazz.jazzflix.config.storage.MinioProperties;
import org.jazz.jazzflix.dto.VideoUploadResponse;
import org.jazz.jazzflix.dto.WatchModel;
import org.jazz.jazzflix.dto.video.VideoAssetDto;
import org.jazz.jazzflix.dto.video.VideoUploadEvent;
import org.jazz.jazzflix.dto.video.VideoTranscodingEvent;
import org.jazz.jazzflix.entity.video.TblVideoAssest;
import org.jazz.jazzflix.exception.InvalidDataException;
import org.jazz.jazzflix.exception.VideoStorageException;
import org.jazz.jazzflix.repository.video.TblVideoAssestRepository;
import org.jazz.jazzflix.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ws.schild.jave.Encoder;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.encode.VideoAttributes;
import ws.schild.jave.info.MultimediaInfo;
import ws.schild.jave.info.VideoInfo;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class VideoUploadServiceImpl implements VideoUploadService {

    private static final Logger log = LoggerFactory.getLogger(VideoUploadServiceImpl.class);

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final MinioService minioService;
    private final TblVideoAssestRepository tblVideoAssestRepository;
    private final KafkaTemplate<String, VideoUploadEvent> kafkaTemplate;
    private final KafkaTemplate<String, VideoTranscodingEvent> transcodingKafkaTemplate;
    private final String videoUploadTopic;
    private final String videoTranscodingTopic;
    private final ProgressService progressService;
    private final VideoTranscodingService transcodingService;
    private final CustomUserDetailsService userDetailsService;

    public VideoUploadServiceImpl(MinioClient minioClient,
                                  MinioProperties minioProperties, MinioService minioService,
                                  TblVideoAssestRepository tblVideoAssestRepository,
                                  KafkaTemplate<String, VideoUploadEvent> kafkaTemplate,
                                  KafkaTemplate<String, VideoTranscodingEvent> transcodingKafkaTemplate,
                                  @Value("${app.kafka.topics.video-upload:video.uploaded}") String videoUploadTopic,
                                  @Value("${app.kafka.topics.video-transcoding:video.transcoding}") String videoTranscodingTopic,
                                  ProgressService progressService,
                                  VideoTranscodingService transcodingService, CustomUserDetailsService userDetailsService) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
        this.minioService = minioService;
        this.tblVideoAssestRepository = tblVideoAssestRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.transcodingKafkaTemplate = transcodingKafkaTemplate;
        this.videoUploadTopic = videoUploadTopic;
        this.videoTranscodingTopic = videoTranscodingTopic;
        this.progressService = progressService;
        this.transcodingService = transcodingService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public VideoUploadResponse handleUpload(MultipartFile file, UUID userId) {
        // Generate unique upload ID for progress tracking
        String uploadId = UUID.randomUUID().toString();

        if (file == null || file.isEmpty()) {
            throw new InvalidDataException("Video file must not be empty");
        }

        String originalFileName = Optional.ofNullable(file.getOriginalFilename())
                .filter(name -> !name.isBlank())
                .orElse("video-" + System.currentTimeMillis());

//         Create progress tracking
        progressService.createProgress(uploadId, originalFileName, file.getSize());

        String objectKey = buildObjectKey(originalFileName);

//        // Create progress-aware multipart file
        ProgressMultipartFile progressFile = new ProgressMultipartFile(file,
            bytesRead -> {
                progressService.updateProgress(uploadId, bytesRead);
            });

        uploadToMinio(progressFile, objectKey, uploadId);

        // Extract and upload thumbnail
        progressService.updateStatus(uploadId, "THUMBNAIL", "Extracting video thumbnail...");

        String thumbnailObjectKey = null;
        try {
            thumbnailObjectKey = extractAndUploadThumbnail(file, objectKey);
        } catch (Exception e) {
            log.warn("Failed to extract thumbnail for video {}, continuing without thumbnail", objectKey, e);
        }

        // Save metadata to database
        progressService.updateStatus(uploadId, "PROCESSING", "Saving video metadata...");

        TblVideoAssest asset = new TblVideoAssest();
        asset.setOriginalFileName(originalFileName);
        asset.setObjectKey(objectKey);
        asset.setContentType(file.getContentType());
        asset.setSizeBytes(file.getSize());
        asset.setStatus("UPLOADED");
        asset.setBucket(minioProperties.getBucket());
        asset.setThumbnailObjectKey(thumbnailObjectKey);
        asset.setOwnerId(userId);

        TblVideoAssest savedAsset = tblVideoAssestRepository.save(asset);

        // Create original quality record
        progressService.updateStatus(uploadId, "TRANSCODING", "Starting video transcoding...");

        // Send transcoding tasks to Kafka for each quality
        List<String> qualities = Arrays.asList("1080p", "720p", "480p", "360p");
        for (String quality : qualities) {
            VideoTranscodingEvent transcodingEvent = new VideoTranscodingEvent(savedAsset.getId(), objectKey, file.getContentType(), quality);
            transcodingKafkaTemplate.send(videoTranscodingTopic, savedAsset.getId().toString() + "-" + quality, transcodingEvent)
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            log.error("Failed to send transcoding event for {} quality {}", savedAsset.getId(), quality, throwable);
                        } else {
                            log.info("Sent transcoding event for {} quality {} to topic {}", savedAsset.getId(), quality, videoTranscodingTopic);
                        }
                    });
        }

        OffsetDateTime uploadedAt = OffsetDateTime.ofInstant(savedAsset.getCreatedAt().toInstant(), ZoneOffset.UTC);

        VideoUploadEvent event = new VideoUploadEvent(
            savedAsset.getId(),
            savedAsset.getObjectKey(),
            savedAsset.getBucket(),
            savedAsset.getSizeBytes(),
            savedAsset.getContentType(),
            uploadedAt
        );

        kafkaTemplate.send(videoUploadTopic, savedAsset.getId().toString(), event)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Failed to publish video upload event for {}", savedAsset.getId(), throwable);
                    } else {
                        log.info("Published video upload event for {} to topic {}", savedAsset.getId(), videoUploadTopic);
                    }
                });

        // Mark upload as completed
        progressService.completeProgress(uploadId, savedAsset.getId().toString());

        String thumbnailUrl = savedAsset.getThumbnailObjectKey() != null
            ? String.format("/jazz/video/api/thumbnail/%s", savedAsset.getId())
            : null;

        WatchModel watchModel = new WatchModel(
                savedAsset.getId(),
                savedAsset.getOriginalFileName(),
                null,
                savedAsset.getCreatedAt().toLocalDateTime(),
                savedAsset.getObjectKey(),
                savedAsset.getContentType(),
                thumbnailUrl,
                null,
                null,
                null
        );

        return new VideoUploadResponse(watchModel, uploadId);
    }

    @Override
    public List<VideoAssetDto> getAllVideosByUserId(UUID userId) {
        List<VideoAssetDto> videoList = new ArrayList<>();
        List<TblVideoAssest> videos = this.tblVideoAssestRepository.findAllByOwnerId(userId);
        videoList = videos.stream()
                .map(video -> {
                    return VideoAssetDto.builder()
                            .id(video.getId())
                            .ownerId(video.getOwnerId())
                            .thumbnail(video.getThumbnailObjectKey())
                            .manifestPath(video.getId().toString())
                            .sizeBytes((int)video.getSizeBytes())
                            .bucket(video.getBucket())
                            .build();
                })
                .toList();
        return videoList;
    }

    private void uploadToMinio(MultipartFile file, String objectKey, String uploadId) {
        progressService.updateStatus(uploadId, "UPLOADING", "Uploading video to storage...");
        minioService.uploadFile(file, objectKey, uploadId);
        progressService.updateStatus(uploadId, "PROCESSING", "Video uploaded, processing metadata...");
//        try {
//
//            PutObjectArgs.Builder builder = PutObjectArgs.builder()
//                    .bucket(minioProperties.getBucket())
//                    .object(objectKey)
//                    .stream(file.getInputStream(), file.getSize(), -1);
//
//            if (file.getContentType() != null) {
//                builder.contentType(file.getContentType());
//            }
//
//            minioClient.putObject(builder.build());
//
//        } catch (IOException ex) {
//            progressService.failProgress(uploadId, "Failed to read video content: " + ex.getMessage());
//            throw new VideoStorageException("Failed to read video content", ex);
//        } catch (Exception ex) {
//            progressService.failProgress(uploadId, "Failed to store video in MinIO: " + ex.getMessage());
//            throw new VideoStorageException("Failed to store video in MinIO", ex);
//        }
    }

    private String buildObjectKey(String originalFileName) {
        String sanitized = originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String extension = "";
        int dotIndex = sanitized.lastIndexOf('.');
        if (dotIndex > -1 && dotIndex < sanitized.length() - 1) {
            extension = sanitized.substring(dotIndex);
        }
        return UUID.randomUUID() + extension;
    }

    private String extractAndUploadThumbnail(MultipartFile videoFile, String videoObjectKey) {
        Path tempVideo = null;
        Path tempThumb = null;

        try {
            tempVideo = Files.createTempFile("video-", ".mp4");
            videoFile.transferTo(tempVideo.toFile());

            double duration = getVideoDurationSeconds(tempVideo);
            double captureAt = Math.min(2.0, duration * 0.1);

            tempThumb = Files.createTempFile("thumb-", ".jpg");

            extractThumbnailWithFFmpeg(tempVideo, tempThumb, captureAt);

//            String thumbnailKey = "thumbnails/" +
//                    videoObjectKey.replaceAll("\\.[^.]+$", ".jpg");
            String thumbnailKey = "thumbnails/" + videoObjectKey.replace(videoObjectKey.substring(videoObjectKey.lastIndexOf('.') + 1), "jpg");


            minioService.uploadThumbnail(thumbnailKey, tempThumb);
            log.info("Successfully uploaded thumbnail {} for video {}", thumbnailKey, videoObjectKey);

            return thumbnailKey;

        } catch (Exception e) {
            log.error("Thumbnail extraction failed for {}", videoObjectKey, e);
            throw new VideoStorageException("Thumbnail extraction failed", e);
        } finally {
           // Clean up temp files
            if (tempVideo != null) {
                try {
                    Files.deleteIfExists(tempVideo);
                } catch (Exception e) {
                    log.warn("Failed to delete temp video file: {}", tempVideo, e);
                }
            }
            if (tempThumb != null) {
                try {
                    Files.deleteIfExists(tempThumb);
                } catch (Exception e) {
                    log.warn("Failed to delete temp thumbnail file: {}", tempThumb, e);
                }
            }
        }
    }

//    private String extractAndUploadThumbnail(MultipartFile videoFile, String videoObjectKey){
//        Path tempVideoPath = null;
//        Path tempThumbnailPath = null;
//
//        try {
//            // Save video to temp file for JAVE processing
//            tempVideoPath = Files.createTempFile("video-", ".tmp");
//            videoFile.transferTo(tempVideoPath.toFile());
//
//            // Get video info
//            MultimediaObject multimediaObject = new MultimediaObject(tempVideoPath.toFile());
//            MultimediaInfo info = multimediaObject.getInfo();
//            VideoInfo videoInfo = info.getVideo();
//
//            if (videoInfo == null) {
//                throw new VideoStorageException("No video stream found in file");
//            }
//
//            long durationMillis = info.getDuration();
//            // Extract frame at 10% of video duration or 2 seconds, whichever is smaller
//            float captureAtSeconds = Math.min(2.0f, (durationMillis / 1000.0f) * 0.1f);
//
//            // Create temp file for thumbnail
//            tempThumbnailPath = Files.createTempFile("thumbnail-", ".jpg");
//
//            // Set up video encoding attributes for thumbnail extraction
//            VideoAttributes videoAttrs = new VideoAttributes();
//            videoAttrs.setCodec("mjpeg");
//            videoAttrs.setSize(info.getVideo().getSize()); // Keep original size
//            videoAttrs.setFrameRate(1);
//
//            EncodingAttributes attrs = new EncodingAttributes();
//            attrs.setOutputFormat("image2");
//            attrs.setDuration(0.001f); // Extract single frame
//            attrs.setOffset(captureAtSeconds);
//            attrs.setVideoAttributes(videoAttrs);
//
//            // Encode/extract the frame
//            Encoder encoder = new Encoder();
//            encoder.encode(multimediaObject, tempThumbnailPath.toFile(), attrs);
//
//            // Upload thumbnail to MinIO
//            String thumbnailObjectKey = "thumbnails/" + videoObjectKey.replace(videoObjectKey.substring(videoObjectKey.lastIndexOf('.') + 1), "jpg");
//            minioService.uploadThumbnail(thumbnailObjectKey, tempThumbnailPath);
////            try (FileInputStream thumbnailStream = new FileInputStream(tempThumbnailPath.toFile())) {
////                long thumbnailSize = Files.size(tempThumbnailPath);
////
////                PutObjectArgs putArgs = PutObjectArgs.builder()
////                        .bucket(minioProperties.getBucket())
////                        .object(thumbnailObjectKey)
////                        .stream(thumbnailStream, thumbnailSize, -1)
////                        .contentType("image/jpeg")
////                        .build();
////
////                minioClient.putObject(putArgs);
////            }
//            log.info("Successfully uploaded thumbnail {} for video {}", thumbnailObjectKey, videoObjectKey);
//
//            return thumbnailObjectKey;
//
//        } catch (Exception e){
//            log.error("Failed Extraction thumbnail for video {}", videoObjectKey, e);
//            throw new VideoStorageException("Failed to extract thumbnail for video " + videoObjectKey, e);
//        } finally {
//            // Clean up temp files
//            if (tempVideoPath != null) {
//                try {
//                    Files.deleteIfExists(tempVideoPath);
//                } catch (Exception e) {
//                    log.warn("Failed to delete temp video file: {}", tempVideoPath, e);
//                }
//            }
//            if (tempThumbnailPath != null) {
//                try {
//                    Files.deleteIfExists(tempThumbnailPath);
//                } catch (Exception e) {
//                    log.warn("Failed to delete temp thumbnail file: {}", tempThumbnailPath, e);
//                }
//            }
//        }
//    }

    private double getVideoDurationSeconds(Path inputFile) throws Exception {
        List<String> cmd = List.of(
                "ffprobe",
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                inputFile.toString()
        );

        Process process = new ProcessBuilder(cmd).start();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            return Double.parseDouble(br.readLine());
        }
    }

    private void extractThumbnailWithFFmpeg(Path inputFile, Path outputImage, double captureAtSeconds){
        try {
            log.info("Extracting thumbnail at {}s from {}", captureAtSeconds, inputFile);
            List<String> command = List.of(
                    "ffmpeg",              // e.g. /usr/local/bin/ffmpeg
                    "-y",                    // overwrite
                    "-ss", String.valueOf(captureAtSeconds), // fast seek
                    "-i", inputFile.toString(),
                    "-frames:v", "1",        // single frame
                    "-q:v", "2",             // high quality
                    "-vf", "scale=iw:-1",    // preserve aspect ratio
                    outputImage.toString()
            );

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            Thread reader = new Thread(() -> {
                try (BufferedReader br = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        log.info("ffmpeg[thumbnail]: {}", line);
                    }
                } catch (Exception ignored) {}
            });
            reader.setDaemon(true);
            reader.start();

            boolean finished = process.waitFor(2, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("FFmpeg thumbnail extraction timed out");
            }

            if (process.exitValue() != 0) {
                throw new RuntimeException("FFmpeg thumbnail extraction failed");
            }
        } catch (InterruptedException | IOException e) {
            log.error("Error extracting thumbnail at {}s from {}", captureAtSeconds, inputFile, e);
            throw new VideoStorageException("Error extracting thumbnail at " + captureAtSeconds, e);
        }

        log.info("Thumbnail created at {}", outputImage);
    }



}
