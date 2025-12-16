# Video Thumbnail Extraction Feature

## Overview
The video upload API now automatically extracts a thumbnail from uploaded videos and stores it in MinIO alongside the video file.

## How It Works

### 1. Video Upload Process
When a video is uploaded via `POST /video/api/upload`:
- The video file is uploaded to MinIO
- **NEW**: A thumbnail is automatically extracted from the video at 10% of the video duration (or 2 seconds, whichever is smaller)
- The thumbnail is saved as a JPEG image in the `thumbnails/` folder in MinIO
- The thumbnail path is stored in the `TBL_VIDEO.thumbnail_object_key` column
- Video metadata is saved to the database
- A Kafka event is published

### 2. Thumbnail Extraction Details
- **Library Used**: JAVE2 (Java Audio Video Encoder) - a Java wrapper for FFmpeg
- **Format**: JPEG image
- **Capture Time**: 10% of video duration or 2 seconds (whichever is smaller)
- **Storage Location**: MinIO bucket under `thumbnails/` folder
- **Naming Convention**: `thumbnails/{video-uuid}.jpg`

### 3. Thumbnail Retrieval
Access the thumbnail via:
```
GET /video/api/thumbnail/{videoId}
```

**Response**: 
- Content-Type: `image/jpeg`
- Returns the thumbnail image as binary data
- Returns 404 if video not found or thumbnail doesn't exist

## API Response Updates

### Upload Response (WatchModel)
The `thumbnail` field in the response now contains the thumbnail URL:
```json
{
  "success": true,
  "message": "Video uploaded successfully.",
  "data": {
    "uuid": "550e8400-e29b-41d4-a716-446655440000",
    "title": "my-video.mp4",
    "description": null,
    "timestamp": "2025-12-12T11:30:00",
    "slug": "550e8400-e29b-41d4-a716-446655440000.mp4",
    "type": "video/mp4",
    "thumbnail": "/api/video/thumbnail/550e8400-e29b-41d4-a716-446655440000",
    "genera": null,
    "rating": null,
    "watchContent": null
  },
  "status": 200,
  "path": "/video/api/upload",
  "timestamp": "2025-12-12T11:30:00"
}
```

## Database Schema

### New Column in TBL_VIDEO
```sql
ALTER TABLE TBL_VIDEO ADD COLUMN thumbnail_object_key VARCHAR(255);
```

This column stores the MinIO object key for the thumbnail (e.g., `thumbnails/550e8400-e29b-41d4-a716-446655440000.jpg`)

## Testing in Postman

### 1. Upload Video with Thumbnail
```
POST http://localhost:8080/jazz/video/api/upload
```
- Method: POST
- Body: form-data
- Key: `file` (type: File)
- Select a video file (MP4, AVI, etc.)

**Expected Response**: JSON with thumbnail URL in the `data.thumbnail` field

### 2. View Thumbnail
```
GET http://localhost:8080/jazz/api/video/thumbnail/{videoId}
```
Replace `{videoId}` with the UUID from the upload response.

**Expected Response**: JPEG image displayed in browser/Postman

## MinIO Verification

1. Access MinIO Console: http://localhost:9001
2. Login: minioadmin / minioadmin123
3. Navigate to `jazzflix-videos` bucket
4. You should see:
   - Video files in the root: `550e8400-e29b-41d4-a716-446655440000.mp4`
   - Thumbnails in `thumbnails/` folder: `thumbnails/550e8400-e29b-41d4-a716-446655440000.jpg`

## Error Handling

### Graceful Degradation
If thumbnail extraction fails:
- The video upload continues successfully
- The `thumbnail_object_key` is set to `null`
- A warning is logged but the upload is not blocked
- The response will have `thumbnail: null`

### Common Issues
1. **Unsupported video format**: Some video codecs may not be supported by JAVE2
2. **Corrupted video**: If the video file is corrupted, thumbnail extraction may fail
3. **Very short videos**: Videos shorter than 0.1 seconds may not generate thumbnails properly

## Dependencies Added

```xml
<!-- JAVE2 for video processing and thumbnail extraction -->
<dependency>
    <groupId>ws.schild</groupId>
    <artifactId>jave-core</artifactId>
    <version>3.5.0</version>
</dependency>
<dependency>
    <groupId>ws.schild</groupId>
    <artifactId>jave-nativebin-win64</artifactId>
    <version>3.5.0</version>
</dependency>
```

**Note**: For Linux deployment, you'll need to add `jave-nativebin-linux64` instead of or alongside `jave-nativebin-win64`.

## Future Enhancements

Potential improvements:
1. Multiple thumbnails at different timestamps
2. Custom thumbnail upload (override auto-generated)
3. Thumbnail size configuration (resolution)
4. Support for video preview clips (GIF/WebM)
5. Async thumbnail generation (via Kafka consumer)
