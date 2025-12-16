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

    @KafkaListener(topics = "${app.kafka.topics.video-transcoding:video.transcoding}", groupId = "transcoding-group")
    public void handleTranscoding(VideoTranscodingEvent event) {
        log.info("Received transcoding event for video {} quality {}", event.getVideoId(), event.getQuality());
        try {
            transcodingService.transcodeVideoQualityAsync(event.getVideoId(), event.getOriginalObjectKey(), event.getContentType(), event.getQuality());
        } catch (Exception e) {
            log.error("Failed to process transcoding for video {} quality {}", event.getVideoId(), event.getQuality(), e);
        }
    }
}