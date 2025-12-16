package org.jazz.jazzflix.service;

import lombok.RequiredArgsConstructor;
import org.jazz.jazzflix.entity.video.TblVideoQuality;
import org.jazz.jazzflix.repository.video.TblVideoQualityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoQualityPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(VideoQualityPersistenceService.class);

    private final TblVideoQualityRepository qualityRepository;

    /**
     * Save a single quality record with simple retry logic.
     */
    @Transactional
    public TblVideoQuality saveQuality(TblVideoQuality quality) {
        int maxAttempts = 3;
        long backoffMs = 500;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                TblVideoQuality saved = qualityRepository.save(quality);
                log.info("Saved TblVideoQuality {} for video {} on attempt {}", saved.getQuality(), saved.getVideoId(), attempt);
                return saved;
            } catch (Exception ex) {
                log.warn("Failed to save TblVideoQuality (attempt {}/{}). Retrying...", attempt, maxAttempts, ex);
                if (attempt == maxAttempts) {
                    log.error("Exceeded max retries saving TblVideoQuality for video {} quality {}", quality.getVideoId(), quality.getQuality(), ex);
                    throw ex;
                }
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while retrying saveQuality", ie);
                }
                backoffMs *= 2;
            }
        }
        return quality; // unreachable
    }

    @Transactional
    public List<TblVideoQuality> saveAll(List<TblVideoQuality> qualities) {
        return qualityRepository.saveAll(qualities);
    }
}
