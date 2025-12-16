# Video Assets Placeholder

This directory should contain video files for the streaming demo.

## Required Files:

- `demo-video.mp4` - Main demo video file (MP4 format)
- `demo-video.webm` - Alternative video format for broader browser support
- `poster.jpg` - Video poster image (shown before playback)
- `thumb1.jpg`, `thumb2.jpg`, `thumb3.jpg`, `thumb4.jpg` - Thumbnail images for video cards

## How to Add Videos:

1. Place your MP4 video files in this directory
2. For best compatibility, use H.264 codec for MP4 files
3. Recommended resolution: 1280x720 (720p) or 1920x1080 (1080p)

## Sample Video Sources:

You can use free sample videos from:
- https://sample-videos.com/
- https://www.pexels.com/videos/
- https://pixabay.com/videos/

## Note:

The application will still work without video files. HTML5 video element will show a fallback message.
Thumbnail images have inline SVG fallbacks that will display if image files are not present.
