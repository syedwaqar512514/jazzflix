package org.jazz.jazzflix.service;

import org.jazz.jazzflix.config.storage.MinioProperties;
import org.jazz.jazzflix.dto.VideoQualityDto;
import org.jazz.jazzflix.entity.video.TblVideoQuality;
import org.jazz.jazzflix.repository.video.TblVideoQualityRepository;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoTranscodingService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final TblVideoQualityRepository qualityRepository;
    private final VideoQualityPersistenceService qualityPersistenceService;

    // Define quality configurations
    public enum VideoQuality {
        ORIGINAL("ORIGINAL", null, null, null),
        Q_1080P("1080p", "1920x1080", "5000k", "1920x1080"),
        Q_720P("720p", "1280x720", "3000k", "1280x720"),
        Q_480P("480p", "854x480", "1500k", "854x480"),
        Q_360P("360p", "640x360", "800k", "640x360");

        public final String name;
        public final String resolution;
        public final String bitrate;
        public final String scale;

        VideoQuality(String name, String resolution, String bitrate, String scale) {
            this.name = name;
            this.resolution = resolution;
            this.bitrate = bitrate;
            this.scale = scale;
        }
    }

//    @Async
//    @Transactional
//    public void transcodeVideoAsync(UUID videoId, String originalObjectKey, String originalContentType) {
//        log.info("Starting async DASH transcoding for video {} with object key {}", videoId, originalObjectKey);
//
//        try {
//            createDashManifest(videoId, originalObjectKey, originalContentType);
//        } catch (Exception e) {
//            log.error("Failed to create DASH manifest for video {}", videoId, e);
//        }
//    }

    @Async
    @Transactional
    public void transcodeVideoQualityAsync(UUID videoId, String originalObjectKey, String originalContentType, String quality) {
        log.info("Starting async transcoding for video {} quality {} with object key {}", videoId, quality, originalObjectKey);

        try {
            createDashForQuality(videoId, originalObjectKey, originalContentType, quality);
        } catch (Exception e) {
            log.error("Failed to transcode video {} for quality {}", videoId, quality, e);
        }
    }

    private void createDashManifest(UUID videoId, String originalObjectKey, String contentType) throws Exception {
        log.info("Creating DASH manifest for video {}", videoId);
        Path tempInputPath = null;
        Path tempOutputDir = null;

        try {
            // Download original video
            log.info("Downloading original video from MinIO: {}", originalObjectKey);
            tempInputPath = Files.createTempFile("input-", ".mp4");
            GetObjectArgs getArgs = GetObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(originalObjectKey)
                    .build();

            try (InputStream inputStream = minioClient.getObject(getArgs);
                 FileOutputStream outputStream = new FileOutputStream(tempInputPath.toFile())) {
                inputStream.transferTo(outputStream);
            }
            log.info("Downloaded video to temp file: {}", tempInputPath);

            // Create temp directory for DASH output
            tempOutputDir = Files.createTempDirectory("dash-output-");
            log.info("Created temp output directory: {}", tempOutputDir);

            // Run FFmpeg to create DASH manifest and segments
            createDashWithFFmpeg(tempInputPath, tempOutputDir);

            // Upload DASH files to MinIO
            //uploadDashFiles(videoId, tempOutputDir);

            // Create database records for DASH qualities
            createDashQualityRecords(videoId, tempOutputDir);

            log.info("Successfully created DASH manifest for video {}", videoId);

        } finally {
            // Cleanup temp files
            if (tempInputPath != null && Files.exists(tempInputPath)) {
                Files.delete(tempInputPath);
            }
            if (tempOutputDir != null && Files.exists(tempOutputDir)) {
                // Log contents before deletion to help debug file locks
                try {
                    long entries = Files.list(tempOutputDir).count();
                    log.info("Deleting temp directory {} with {} entries", tempOutputDir, entries);
                } catch (Exception e) {
                    log.info("Deleting temp directory {}", tempOutputDir);
                }

                // Delete temp directory and contents
                Files.walk(tempOutputDir)
                    .sorted((a, b) -> b.compareTo(a)) // reverse order
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (Exception e) {
                            log.warn("Failed to delete temp file: {}", path, e);
                        }
                    });
            }
        }
    }

    private void createDashWithFFmpeg(Path inputFile, Path outputDir) throws Exception {
        log.info("Running FFmpeg to create DASH manifest");

        // FFmpeg command for multi-bitrate DASH
        // This creates multiple representations in one command
        List<String> command = Arrays.asList(
            "ffmpeg",
            "-i", inputFile.toString(),
            // Video representations
            "-map", "0:v",
            "-c:v", "libx264",
            "-b:v:0", "5000k", "-s:v:0", "1920x1080", // 1080p
            "-b:v:1", "3000k", "-s:v:1", "1280x720",  // 720p
            "-b:v:2", "1500k", "-s:v:2", "854x480",   // 480p
            "-b:v:3", "800k",  "-s:v:3", "640x360",   // 360p
            // Audio representation
            "-map", "0:a",
            "-c:a", "aac",
            "-b:a", "128k",
            // DASH options
            "-f", "dash",
            "-seg_duration", "10",
            "-use_template", "1",
            "-use_timeline", "1",
            "-init_seg_name", "init-$RepresentationID$.m4s",
            "-media_seg_name", "chunk-$RepresentationID$-$Number$.m4s",
            "-adaptation_sets", "id=0,streams=v id=1,streams=a",
            outputDir.resolve("manifest.mpd").toString()
        );

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // Consume process output to avoid buffer deadlock
        Thread outputReader = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    log.info("ffmpeg: {}", line);
                }
            } catch (Exception e) {
                log.warn("Error reading ffmpeg output", e);
            }
        }, "ffmpeg-output-reader");
        outputReader.setDaemon(true);
        outputReader.start();

        try {
            // Wait for completion with timeout to prevent indefinite hang
            boolean finished = process.waitFor(30, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("FFmpeg timed out and was killed");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new RuntimeException("FFmpeg failed with exit code: " + exitCode);
            }

            log.info("FFmpeg DASH creation completed successfully");
        } finally {
            // Ensure reader thread finishes and process streams are closed
            try {
                outputReader.join(5000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            try {
                process.getInputStream().close();
            } catch (Exception ignored) {}
            try {
                process.getErrorStream().close();
            } catch (Exception ignored) {}
            try {
                process.getOutputStream().close();
            } catch (Exception ignored) {}
        }
    }

    private void createDashWithFFmpegForQuality(Path inputFile, Path outputDir, String quality) throws Exception {
        log.info("Running FFmpeg to create DASH for quality {}", quality);

        VideoQuality videoQuality = VideoQuality.valueOf("Q_" + quality.toUpperCase());

        // FFmpeg command for single bitrate DASH
        List<String> command = Arrays.asList(
            "ffmpeg",
            "-i", inputFile.toString(),
            // Video representation
            "-map", "0:v",
            "-c:v", "libx264",
            "-b:v", videoQuality.bitrate, "-s:v", videoQuality.scale,
            // Audio representation
            "-map", "0:a",
            "-c:a", "aac",
            "-b:a", "128k",
            // DASH options
            "-f", "dash",
            "-seg_duration", "10",
            "-use_template", "1",
            "-use_timeline", "1",
            "-init_seg_name", "init-$RepresentationID$.m4s",
            "-media_seg_name", "chunk-$RepresentationID$-$Number$.m4s",
            "-adaptation_sets", "id=0,streams=v id=1,streams=a",
            outputDir.resolve("manifest.mpd").toString()
        );

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        Thread outputReader = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    log.info("ffmpeg[{}]: {}", quality, line);
                }
            } catch (Exception e) {
                log.warn("Error reading ffmpeg output for quality {}", quality, e);
            }
        }, "ffmpeg-output-reader-" + quality);
        outputReader.setDaemon(true);
        outputReader.start();


        try {
            boolean finished = process.waitFor(20, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("FFmpeg timed out for quality " + quality + " and was killed");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new RuntimeException("FFmpeg failed for quality " + quality + " with exit code: " + exitCode);
            }

            log.info("FFmpeg DASH creation for quality {} completed successfully", quality);
        } finally {
            try {
                outputReader.join(5000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            try {
                process.getInputStream().close();
            } catch (Exception ignored) {}
            try {
                process.getErrorStream().close();
            } catch (Exception ignored) {}
            try {
                process.getOutputStream().close();
            } catch (Exception ignored) {}
        }
    }

    private void uploadDashFiles(UUID videoId, Path outputDir, String quality) throws Exception {
        log.info("Uploading DASH files to MinIO for video {}", videoId);
        String bucket = resolveBucketForQuality(quality);
        String basePath = "videos/" + videoId + "/dash/";
        Files.walk(outputDir)
            .filter(Files::isRegularFile)
            .forEach(filePath -> {
                String fileName = filePath.getFileName().toString();
                String objectKey = basePath + fileName;
                try {
                    long size = Files.size(filePath);
                    try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
                        PutObjectArgs putArgs = PutObjectArgs.builder()
                                .bucket(bucket)
                                .object(objectKey)
                                .stream(fis, size, -1)
                                .contentType(getContentType(fileName))
                                .build();

                        minioClient.putObject(putArgs);
                        log.info("Uploaded DASH file: {} to {} bucket", objectKey, bucket);
                    }
                } catch (Exception e) {
                    log.error("Failed to upload DASH file: {} to {} bucket", filePath, bucket, e);
                }
            });
    }

    private String getContentType(String fileName) {
        if (fileName.endsWith(".mpd")) {
            return "application/dash+xml";
        } else if (fileName.endsWith(".m4s")) {
            return "video/iso.segment";
        }
        return "application/octet-stream";
    }

    @Transactional
    protected void createDashQualityRecords(UUID videoId, Path outputDir) throws Exception {
        log.info("Creating DASH quality records for video {}", videoId);

        String manifestKey = "videos/" + videoId + "/dash/manifest.mpd";

        // Create records for each quality representation
        List<VideoQuality> qualities = Arrays.asList(
            VideoQuality.Q_1080P,
            VideoQuality.Q_720P,
            VideoQuality.Q_480P,
            VideoQuality.Q_360P
        );

        for (VideoQuality quality : qualities) {
            TblVideoQuality videoQuality = new TblVideoQuality();
            videoQuality.setVideoId(videoId);
            videoQuality.setQuality(quality.name);
            videoQuality.setResolution(quality.resolution);
            videoQuality.setBitrate(quality.bitrate);
            videoQuality.setObjectKey(manifestKey); // All qualities point to the same manifest
            videoQuality.setSizeBytes(0L); // Size not applicable for manifest
            videoQuality.setContentType("application/dash+xml");
            videoQuality.setStatus("COMPLETED");

            qualityPersistenceService.saveQuality(videoQuality);
            log.info("Created DASH quality record: {} for video {}", quality.name, videoId);
        }
    }


    protected void createDashQualityRecord(UUID videoId, Path outputDir, String quality) throws Exception {
        log.info("Creating DASH quality record for video {} quality {}", videoId, quality);

        String manifestKey = "videos/" + videoId + "/dash/manifest.mpd";
        String qualityBucket = "videos-q"+quality.toLowerCase();
        VideoQuality vQuality = VideoQuality.valueOf("Q_"+quality.toUpperCase());

        TblVideoQuality videoQuality = new TblVideoQuality();
        videoQuality.setVideoId(videoId);
        videoQuality.setQuality(vQuality.name);
        videoQuality.setResolution(vQuality.resolution);
        videoQuality.setBitrate(vQuality.bitrate);
        videoQuality.setObjectKey(manifestKey); // All qualities point to the same manifest
        videoQuality.setSizeBytes(0L); // Size not applicable for manifest
        videoQuality.setContentType("application/dash+xml");
        videoQuality.setQualityBucketName(qualityBucket);
        videoQuality.setStatus("COMPLETED");

        qualityPersistenceService.saveQuality(videoQuality);
        log.info("Created DASH quality record: {} for video {}", vQuality.name, videoId);
    }

    private void createDashForQuality(UUID videoId, String originalObjectKey, String contentType, String quality) throws Exception {
        log.info("Creating DASH for video {} quality {}", videoId, quality);
        Path tempInputPath = null;
        Path tempOutputDir = null;

        try {
            // Download original video
            log.info("Downloading original video from MinIO: {}", originalObjectKey);
            tempInputPath = Files.createTempFile("input-", ".mp4");
            GetObjectArgs getArgs = GetObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(originalObjectKey)
                    .build();

            try (InputStream inputStream = minioClient.getObject(getArgs);
                 FileOutputStream outputStream = new FileOutputStream(tempInputPath.toFile())) {
                inputStream.transferTo(outputStream);
            }
            log.info("Downloaded video to temp file: {}", tempInputPath);

            // Create temp directory for DASH output
            tempOutputDir = Files.createTempDirectory(quality+"_dash-quality-");
            log.info("Created temp output directory: {}", tempOutputDir);

            // Run FFmpeg to create DASH for this quality
            createDashWithFFmpegForQuality(tempInputPath, tempOutputDir, quality);

            // Upload DASH files to MinIO
            uploadDashFiles(videoId, tempOutputDir, quality);

            // Create database record for this quality
            createDashQualityRecord(videoId, tempOutputDir, quality);

            log.info("Successfully created DASH for video {} quality {}", videoId, quality);

        } finally {
            // Cleanup temp files
            if (tempInputPath != null && Files.exists(tempInputPath)) {
                Files.delete(tempInputPath);
            }
            if (tempOutputDir != null && Files.exists(tempOutputDir)) {
                // Log contents before deletion to help debug file locks
                try {
                    long entries = Files.list(tempOutputDir).count();
                    log.info("Deleting temp directory {} with {} entries", tempOutputDir, entries);
                } catch (Exception e) {
                    log.info("Deleting temp directory {}", tempOutputDir);
                }

                // Delete temp directory and contents
                Files.walk(tempOutputDir)
                    .sorted((a, b) -> b.compareTo(a)) // reverse order
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (Exception e) {
                            log.warn("Failed to delete temp file: {}", path, e);
                        }
                    });
            }
        }
    }

    public List<VideoQualityDto> getVideoQualities(UUID videoId) {
        List<TblVideoQuality> qualities = qualityRepository.findByVideoId(videoId);
        return qualities.stream()
                .map(this::convertToDto)
                .toList();
    }

    private VideoQualityDto convertToDto(TblVideoQuality entity) {
        String downloadUrl = String.format("/jazz/video/api/download/%s/%s",
                entity.getVideoId(), entity.getQuality().toLowerCase());

        return new VideoQualityDto(
                entity.getQuality(),
                entity.getResolution(),
                entity.getBitrate(),
                entity.getSizeBytes(),
                entity.getStatus(),
                entity.getCreatedAt().toLocalDateTime(),
                downloadUrl
        );
    }

    public String resolveBucketForQuality(String quality){
        return this.minioProperties.getQualityBuckets().get(quality.toLowerCase());
    }

}