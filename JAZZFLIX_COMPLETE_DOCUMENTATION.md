# 🎬 JazzFlix Video Upload System - Complete Documentation

## Overview
JazzFlix is a comprehensive video upload and management system built with Spring Boot 3.x, featuring automatic thumbnail extraction, MinIO object storage, PostgreSQL persistence, and Kafka event streaming.

## 🏗️ System Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Postman       │───▶│  Spring Boot    │───▶│   MinIO         │───▶│   PostgreSQL    │
│   Client        │    │   Application   │    │   Object Store  │    │   Database      │
└─────────────────┘    └─────────────────┘    └─────────────────┘    └─────────────────┘
                              │                        │                        │
                              ▼                        ▼                        ▼
                       ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
                       │   JAVE2         │    │   Kafka         │    │   Liquibase     │
                       │   Thumbnail     │    │   Events        │    │   Migrations    │
                       │   Extraction    │    │                 │    │                 │
                       └─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 📋 Prerequisites

### Docker Services (via docker-compose.yml)
- **PostgreSQL 15**: Database for metadata storage
- **Kafka (Confluent 7.6.1)**: Event streaming platform
- **Zookeeper**: Kafka coordination service
- **MinIO**: S3-compatible object storage

### Dependencies
- Java 21
- Maven 3.8+
- FFmpeg (for JAVE2 thumbnail extraction)

## 🚀 Quick Start

### 1. Start Infrastructure
```bash
docker-compose up -d
```

### 2. Start Application
```bash
mvn spring-boot:run
```

### 3. Verify Services
- **Application:** http://localhost:8089/jazz
- **MinIO Console:** http://localhost:9001 (minioadmin/minioadmin123)
- **Kafka UI:** http://localhost:8080 (if configured)

## 📡 API Endpoints

### Video Upload
```http
POST /jazz/video/api/upload
Content-Type: multipart/form-data

Parameter: file (MultipartFile)
Response: JSON with video metadata and thumbnail URL
```

### Get Upload Progress
```http
GET /jazz/video/api/progress/{uploadId}
Content-Type: application/json

Response: ProgressInfo JSON object
```

### WebSocket Real-time Updates
```javascript
// Connect to WebSocket
const socket = new SockJS('/jazz/ws/progress');
const stompClient = Stomp.over(socket);

// Subscribe to progress updates
stompClient.subscribe('/topic/progress/{uploadId}', function(message) {
    const progress = JSON.parse(message.body);
    updateProgressUI(progress);
});
```

### Get Thumbnail
```http
GET /jazz/video/api/thumbnail/{videoId}
Content-Type: image/jpeg

Response: JPEG thumbnail image
```

### Get Video Qualities
```http
GET /jazz/video/api/qualities/{videoId}
Content-Type: application/json

Response: JSON with list of available video qualities
```

### Download Specific Quality
```http
GET /jazz/video/api/download/{videoId}/{quality}
Content-Type: video/mp4

Parameters:
- videoId: UUID of the video
- quality: "original", "1080p", "720p", "480p", "360p"

Response: Video file download
```

## 🎬 Video Quality Transcoding

### Overview
JazzFlix automatically transcodes uploaded videos into multiple quality variants to optimize playback across different devices and network conditions. The transcoding process runs asynchronously after the initial upload completes.

### Supported Qualities
- **ORIGINAL**: Source video quality (no transcoding)
- **1080p**: Full HD (1920x1080, ~4-6 Mbps bitrate)
- **720p**: HD (1280x720, ~2-4 Mbps bitrate)
- **480p**: SD (854x480, ~1-2 Mbps bitrate)
- **360p**: Mobile (640x360, ~800 Kbps - 1.5 Mbps bitrate)

### Transcoding Process
1. **Upload Completion**: Original video stored in MinIO
2. **Async Transcoding**: Background process converts to all quality variants
3. **Quality Storage**: Each variant stored with quality-specific object keys
4. **Database Tracking**: Quality records created for each variant
5. **Status Updates**: Progress tracked via WebSocket during transcoding

### Database Schema (TBL_VIDEO_QUALITY)
```sql
CREATE TABLE TBL_VIDEO_QUALITY (
    id UUID PRIMARY KEY,
    video_id UUID NOT NULL,
    quality VARCHAR(20) NOT NULL,
    resolution VARCHAR(20) NOT NULL,
    bitrate VARCHAR(20) NOT NULL,
    object_key VARCHAR(255) NOT NULL UNIQUE,
    size_bytes BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (video_id) REFERENCES TBL_VIDEO(id) ON DELETE CASCADE
);
```

### Storage Structure with Qualities
```
jazzflix-videos/
├── videos/
│   ├── cfbba5e2-039f-4ced-8557-7aa82aa95457/
│   │   ├── original.mp4
│   │   ├── 1080p.mp4
│   │   ├── 720p.mp4
│   │   ├── 480p.mp4
│   │   └── 360p.mp4
│   └── a1b2c3d4-e5f6-7890-abcd-ef1234567890/
│       ├── original.mp4
│       ├── 1080p.mp4
│       ├── 720p.mp4
│       ├── 480p.mp4
│       └── 360p.mp4
└── thumbnails/
    ├── cfbba5e2-039f-4ced-8557-7aa82aa95457.jpg
    └── a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg
```

### Quality API Response
```json
{
    "videoId": "cfbba5e2-039f-4ced-8557-7aa82aa95457",
    "qualities": [
        {
            "quality": "ORIGINAL",
            "resolution": "1920x1080",
            "bitrate": "8000 kbps",
            "sizeBytes": 104857600,
            "status": "COMPLETED",
            "downloadUrl": "/jazz/video/api/download/cfbba5e2-039f-4ced-8557-7aa82aa95457/original"
        },
        {
            "quality": "Q_1080P",
            "resolution": "1920x1080",
            "bitrate": "5000 kbps",
            "sizeBytes": 52428800,
            "status": "COMPLETED",
            "downloadUrl": "/jazz/video/api/download/cfbba5e2-039f-4ced-8557-7aa82aa95457/1080p"
        },
        {
            "quality": "Q_720P",
            "resolution": "1280x720",
            "bitrate": "2500 kbps",
            "sizeBytes": 26214400,
            "status": "PROCESSING",
            "downloadUrl": null
        }
    ]
}
```

### Transcoding Implementation
- **Library**: JAVE2 (ws.schild:jave-core:3.5.0)
- **Codec**: H.264 video with AAC audio
- **Container**: MP4 format
- **Async Processing**: CompletableFuture for non-blocking transcoding
- **Error Handling**: Failed transcoding doesn't affect other qualities
- **Progress Tracking**: WebSocket updates during transcoding phases

### Performance Considerations
- **CPU Intensive**: Transcoding uses significant CPU resources
- **Memory Usage**: Temporary files created during conversion
- **Storage Growth**: ~3-5x increase in storage per video
- **Concurrent Limits**: Single video transcoded at a time per instance
- **Cleanup**: Automatic temp file deletion after transcoding

## 🔄 Complete Upload Flow

### Step 1: File Reception & Progress Initialization
- Multipart file validation
- Generate unique `uploadId` for progress tracking
- Create progress tracking entry
- Initialize WebSocket broadcasting

### Step 2: Upload Progress Tracking
```java
// Progress-aware file upload
ProgressMultipartFile progressFile = new ProgressMultipartFile(file,
    bytesRead -> {
        progressService.updateProgress(uploadId, bytesRead);
        progressWebSocketController.broadcastProgressUpdate(uploadId);
    });
```

### Step 3: Real-time Progress Updates
- **WebSocket**: `/topic/progress/{uploadId}`
- **REST API**: `GET /jazz/video/api/progress/{uploadId}`
- **Status Updates**: UPLOADING → PROCESSING → THUMBNAIL → COMPLETED

### Step 4: Video Processing Pipeline
- MinIO storage upload with progress tracking
- Automatic thumbnail extraction (JAVE2)
- Database metadata persistence
- Kafka event publishing

### Step 5: Completion & Cleanup
- Mark progress as COMPLETED
- Return video metadata with thumbnail URL
- Automatic cleanup of progress entries

## 🗄️ Database Schema

### TBL_VIDEO Table
```sql
CREATE TABLE TBL_VIDEO (
    id UUID PRIMARY KEY,
    original_file_name VARCHAR(255) NOT NULL,
    object_key VARCHAR(255) NOT NULL UNIQUE,
    content_type VARCHAR(150),
    size_bytes BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    bucket VARCHAR(120) NOT NULL,
    thumbnail_object_key VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);
```

### Liquibase Migrations
- **changeset-1:** Create TBL_USER table
- **changeset-2:** Create TBL_VIDEO table
- **changeset-3:** Add thumbnail_object_key column

## 📁 MinIO Storage Structure

```
jazzflix-videos/
├── videos/
│   ├── cfbba5e2-039f-4ced-8557-7aa82aa95457.mp4
│   └── a1b2c3d4-e5f6-7890-abcd-ef1234567890.mp4
└── thumbnails/
    ├── cfbba5e2-039f-4ced-8557-7aa82aa95457.jpg
    └── a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg
```

## ⚙️ Configuration

### application.properties
```properties
# Server
server.servlet.context-path=/jazz
server.port=8089

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/jazz-flix
spring.datasource.username=postgres
spring.datasource.password=1234
spring.jpa.hibernate.ddl-auto=none

# MinIO
app.minio.endpoint=http://localhost:9000
app.minio.access-key=minioadmin
app.minio.secret-key=minioadmin123
app.minio.bucket=jazzflix-videos
app.minio.ensure-bucket=true

# Kafka
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
app.kafka.topics.video-upload=video.uploaded

# Liquibase
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.xml
```

### Docker Compose Services
```yaml
version: '3.8'
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: jazz-flix
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: 1234
    ports:
      - "5432:5432"

  kafka:
    image: confluentinc/cp-kafka:7.6.1
    depends_on:
      - zookeeper
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    ports:
      - "9092:9092"

  minio:
    image: minio/minio
    environment:
      MINIO_ACCESS_KEY: minioadmin
      MINIO_SECRET_KEY: minioadmin123
    command: server /data
    ports:
      - "9000:9000"
      - "9001:9001"
```

## 📊 Progress Tracking System

### Progress States
- **UPLOADING**: File upload in progress
- **PROCESSING**: Upload complete, processing metadata
- **THUMBNAIL**: Extracting video thumbnail
- **COMPLETED**: Upload fully completed
- **FAILED**: Upload failed with error

### Progress Data Structure
```json
{
    "uploadId": "uuid-string",
    "fileName": "video.mp4",
    "totalBytes": 10485760,
    "uploadedBytes": 5242880,
    "progressPercentage": 50,
    "status": "UPLOADING",
    "message": "Uploading... 50% (5MB/10MB bytes)",
    "startTime": "2025-12-12T11:40:54",
    "lastUpdateTime": "2025-12-12T11:41:00",
    "videoId": null
}
```

### Client Implementation Example

#### HTML Progress UI
```html
<div class="progress-section">
    <div class="status-info" id="statusInfo">Initializing...</div>
    <div class="progress-bar">
        <div class="progress-fill" id="progressFill"></div>
    </div>
    <div id="progressText">0%</div>
</div>
```

#### JavaScript WebSocket Client
```javascript
// Connect to WebSocket
const socket = new SockJS('/jazz/ws/progress');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function() {
    // Subscribe to progress updates
    stompClient.subscribe('/topic/progress/' + uploadId, function(message) {
        const progress = JSON.parse(message.body);
        updateProgressUI(progress);
    });
});

// Update UI with progress
function updateProgressUI(progress) {
    document.getElementById('progressFill').style.width = progress.progressPercentage + '%';
    document.getElementById('progressText').textContent = progress.progressPercentage + '%';
    document.getElementById('statusInfo').textContent = progress.message;
}
```

#### REST API Polling (Fallback)
```javascript
function pollProgress() {
    fetch(`/jazz/video/api/progress/${uploadId}`)
        .then(response => response.json())
        .then(progress => updateProgressUI(progress));
}

// Poll every 2 seconds
setInterval(pollProgress, 2000);
```

## 🛡️ Error Handling

### Video Upload Failures
- **Invalid file:** `InvalidDataException`
- **MinIO connection:** `VideoStorageException`
- **Database error:** `DataAccessException`
- **Thumbnail extraction:** Warning logged, upload continues

### Thumbnail Retrieval Failures
- **Video not found:** 404 Not Found
- **No thumbnail:** 404 Not Found
- **MinIO error:** 500 Internal Server Error

## 🔧 Key Components

### Service Layer (VideoUploadServiceImpl)
- File validation and processing
- MinIO upload coordination
- Thumbnail extraction orchestration
- Database persistence
- Kafka event publishing

### Controller Layer (VideoUploadController)
- REST endpoint handling
- Multipart file processing
- Thumbnail serving
- Error response formatting

### Configuration Classes
- **MinioConfig:** MinIO client bean creation
- **MinioProperties:** Configuration properties
- **SecurityConfig:** Open security configuration

### Entity Model (TblVideoAssest)
- JPA entity mapping
- UUID primary key generation
- Timestamp handling

## 📊 Performance Considerations

### Thumbnail Extraction
- **Memory usage:** Temporary files for video processing
- **CPU intensive:** FFmpeg encoding operations
- **I/O operations:** File system access for temp files
- **Cleanup:** Automatic temp file deletion

### Concurrent Uploads
- **Thread safety:** UUID generation for unique keys
- **Database transactions:** ACID compliance
- **MinIO operations:** Bucket/object isolation

### Storage Optimization
- **Object naming:** UUID-based unique identifiers
- **Thumbnail sizing:** Original video dimensions preserved
- **Format efficiency:** JPEG compression for thumbnails

## 🚀 Production Deployment

### Environment Variables
```bash
# Database
DB_URL=jdbc:postgresql://prod-db:5432/jazzflix
DB_USER=prod_user
DB_PASS=secure_password

# MinIO
MINIO_ENDPOINT=https://minio.prod.com
MINIO_ACCESS_KEY=prod_access_key
MINIO_SECRET_KEY=prod_secret_key

# Kafka
KAFKA_SERVERS=kafka1:9092,kafka2:9092,kafka3:9092
```

### Security Enhancements
- JWT authentication implementation
- API rate limiting
- CORS policy refinement
- Input validation strengthening

### Monitoring & Observability
- Application metrics (Micrometer)
- Health checks (/actuator/health)
- Log aggregation
- Performance monitoring

## 🔮 Future Enhancements

### Video Processing Pipeline
- **Transcoding:** Multiple format support (H.264, VP9)
- **Resolution variants:** 720p, 1080p, 4K
- **Adaptive streaming:** HLS/DASH support

### Advanced Features
- **Video analytics:** Duration, bitrate, codec detection
- **Content moderation:** AI-powered filtering
- **CDN integration:** Global content delivery
- **Playback tracking:** View analytics

### API Extensions
- **Bulk upload:** Multiple file support
- **Upload progress:** Real-time progress tracking
- **Video management:** CRUD operations
- **Search & filtering:** Metadata-based queries

## 📞 Support & Troubleshooting

### Common Issues
1. **Port conflicts:** Check for running processes on 8089, 5432, 9092, 9000
2. **MinIO connection:** Verify credentials and network connectivity
3. **Thumbnail failures:** Check FFmpeg installation and video format support
4. **Database migrations:** Ensure Liquibase runs successfully on startup

### Logs & Debugging
```bash
# Enable debug logging
logging.level.org.jazz.jazzflix=DEBUG
logging.level.io.minio=DEBUG
logging.level.org.springframework.kafka=DEBUG
```

### Health Checks
- **Application:** http://localhost:8089/jazz/actuator/health
- **Database:** Check PostgreSQL connectivity
- **MinIO:** Verify bucket access
- **Kafka:** Test topic creation/consumption

---

**Built with:** Spring Boot 3.1.6, Java 21, PostgreSQL, Kafka, MinIO, JAVE2
**Last Updated:** December 12, 2025
**Version:** 1.0.0</content>
<parameter name="filePath">d:\Jazz_Dev\JazzFlix\JAZZFLIX_COMPLETE_DOCUMENTATION.md