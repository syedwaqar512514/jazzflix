package org.jazz.jazzflix.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import lombok.extern.slf4j.Slf4j;
import org.jazz.jazzflix.config.storage.MinioProperties;
import org.jazz.jazzflix.exception.VideoStorageException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Slf4j
@Service
public class MinioService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final ProgressService progressService;


    public MinioService(MinioClient minioClient, MinioProperties minioProperties, ProgressService progressService) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
        this.progressService = progressService;
    }

    public void uploadFile(MultipartFile file, String objectKey, String uploadId) {
        String objectPath = "/"+objectKey.split("\\.")[0]+"/"+objectKey;
        try {
                PutObjectArgs.Builder builder = PutObjectArgs.builder()
                        .bucket(minioProperties.getBucket())
                        .object(objectPath)
                        .stream(file.getInputStream(), file.getSize(), -1);

                if (file.getContentType() != null) {
                    builder.contentType(file.getContentType());
                }

                minioClient.putObject(builder.build());
        } catch (IOException ex) {
            progressService.failProgress(uploadId, "Failed to read video content: " + ex.getMessage());
            throw new VideoStorageException("Failed to read video content", ex);
        } catch (Exception ex) {
            progressService.failProgress(uploadId, "Failed to store video in MinIO: " + ex.getMessage());
            throw new VideoStorageException("Failed to store video in MinIO", ex);
        }
    }

    public void uploadThumbnail(String thumbnailObjectKey, Path tempThumbnailPath){
        try (FileInputStream thumbnailStream = new FileInputStream(tempThumbnailPath.toFile())) {
            long thumbnailSize = Files.size(tempThumbnailPath);
            PutObjectArgs putArgs = PutObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(thumbnailObjectKey)
                    .stream(thumbnailStream, thumbnailSize, -1)
                    .contentType("image/jpeg")
                    .build();
            minioClient.putObject(putArgs);

        } catch (IOException ex) {
            log.error("Failed upload thumbnail for thumbnailObjectKey {} to MinIO", thumbnailObjectKey, ex);
            throw new VideoStorageException("Failed to upload video thumbail to MinIO", ex);
        } catch (Exception ex) {

            throw new VideoStorageException("Failed to upload video to MinIO", ex);
        }
    }
}
