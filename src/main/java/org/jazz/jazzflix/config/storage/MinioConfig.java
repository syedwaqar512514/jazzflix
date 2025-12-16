package org.jazz.jazzflix.config.storage;

import io.minio.MinioClient;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {

    private static final Logger log = LoggerFactory.getLogger(MinioConfig.class);

    @Bean
    public MinioClient minioClient(MinioProperties properties) {
        try {
            MinioClient client = MinioClient.builder()
                    .endpoint(properties.getEndpoint())
                    .credentials(properties.getAccessKey(), properties.getSecretKey())
                    .build();

            if (properties.isEnsureBucket()) {
                try {
                    ensureBucketExists(client, properties.getBucket());
                } catch (Exception ex) {
                    log.warn("Failed to verify/create MinIO bucket '{}'. MinIO may not be available. Error: {}", 
                            properties.getBucket(), ex.getMessage());
                }
            }
            return client;
        } catch (Exception ex) {
            log.error("Failed to initialize MinIO client. Ensure MinIO is running and credentials are correct.", ex);
            throw new IllegalStateException("Failed to initialize MinIO client", ex);
        }
    }

    private void ensureBucketExists(MinioClient client, String bucketName) throws Exception {
        if (bucketName == null || bucketName.isBlank()) {
            throw new IllegalArgumentException("MinIO bucket name must be provided");
        }
        boolean bucketExists = client.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!bucketExists) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            log.info("Created MinIO bucket '{}'.", bucketName);
        }
    }
}
