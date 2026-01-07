package org.jazz.jazzflix.config.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@Setter
@Getter
@ConfigurationProperties(prefix = "app.minio")
public class MinioProperties {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;
    private String baselineBucket;
    private boolean ensureBucket = true;
    private Map<String, String> qualityBuckets;
    private String metadataBucket;
}
