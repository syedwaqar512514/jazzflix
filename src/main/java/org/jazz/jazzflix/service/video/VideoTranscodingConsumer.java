package org.jazz.jazzflix.service.video;

import org.jazz.jazzflix.dto.video.VideoTranscodingEvent;
import org.jazz.jazzflix.service.VideoTranscodingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class VideoTranscodingConsumer {

    private static final Logger log = LoggerFactory.getLogger(VideoTranscodingConsumer.class);

    private final VideoTranscodingService transcodingService;

    public VideoTranscodingConsumer(VideoTranscodingService transcodingService) {
        this.transcodingService = transcodingService;
    }

    @KafkaListener(topics = "${app.kafka.topics.video-transcoding-single-bitrate:video.transcoding.single.bitrate}", groupId = "transcoding-group")
    public void handleTranscodingSingleBitrate(VideoTranscodingEvent event) {
        log.info("Received transcoding event for video {} quality {}", event.getVideoId(), event.getQuality());
        try {
            // Transconding for Single bitrate (Quality)
//            transcodingService.transcodeVideoQualityAsync(event.getVideoId(), event.getOriginalObjectKey(), event.getContentType(), event.getQuality());
            // Transconding for Multi bitrate (Quality)
            transcodingService.transcodeVideoAsync(event.getVideoId(), event.getOriginalObjectKey(), event.getContentType(), event.getQuality());
        } catch (Exception e) {
            log.error("Failed to process transcoding for video {} quality {}", event.getVideoId(), event.getQuality(), e);
        }
    }

    @KafkaListener(topics = "${app.kafka.topics.video-transcoding-multi-bitrate:video.transcoding.multi.bitrate}", groupId = "transcoding-group")
    public void handleTranscodingMultiBitrate(VideoTranscodingEvent event) {
        log.info("Received transcoding event for video {} quality {}", event.getVideoId(), event.getQuality());
        try {
            // Transconding for Single bitrate (Quality)
//            transcodingService.transcodeVideoQualityAsync(event.getVideoId(), event.getOriginalObjectKey(), event.getContentType(), event.getQuality());
            // Transconding for Multi bitrate (Quality)
            transcodingService.transcodeVideoAsync(event.getVideoId(), event.getOriginalObjectKey(), event.getContentType());
        } catch (Exception e) {
            log.error("Failed to process transcoding for video {} quality {}", event.getVideoId(), event.getQuality(), e);
        }
    }
}