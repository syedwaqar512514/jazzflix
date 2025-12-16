package org.jazz.jazzflix.service;

import org.jazz.jazzflix.dto.ProgressInfo;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class ProgressService {

    private final Map<String, ProgressInfo> progressMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newScheduledThreadPool(1);

    public ProgressService() {
        // Clean up old progress entries every 30 minutes
        cleanupExecutor.scheduleAtFixedRate(this::cleanupOldEntries, 30, 30, TimeUnit.MINUTES);
    }

    public ProgressInfo createProgress(String uploadId, String fileName, long totalBytes) {
        ProgressInfo progress = new ProgressInfo(uploadId, fileName, totalBytes);
        progressMap.put(uploadId, progress);
        return progress;
    }

    public ProgressInfo getProgress(String uploadId) {
        return progressMap.get(uploadId);
    }

    public void updateProgress(String uploadId, long bytesUploaded) {
        ProgressInfo progress = progressMap.get(uploadId);
        if (progress != null) {
            progress.updateProgress(bytesUploaded);
        }
    }

    public void updateStatus(String uploadId, String status, String message) {
        ProgressInfo progress = progressMap.get(uploadId);
        if (progress != null) {
            progress.setStatus(status, message);
        }
    }

    public void completeProgress(String uploadId, String videoId) {
        ProgressInfo progress = progressMap.get(uploadId);
        if (progress != null) {
            progress.complete(videoId);
            // Keep completed entries for 1 hour before cleanup
            cleanupExecutor.schedule(() -> progressMap.remove(uploadId), 1, TimeUnit.HOURS);
        }
    }

    public void failProgress(String uploadId, String errorMessage) {
        ProgressInfo progress = progressMap.get(uploadId);
        if (progress != null) {
            progress.fail(errorMessage);
            // Keep failed entries for 30 minutes before cleanup
            cleanupExecutor.schedule(() -> progressMap.remove(uploadId), 30, TimeUnit.MINUTES);
        }
    }

    public void removeProgress(String uploadId) {
        progressMap.remove(uploadId);
    }

    private void cleanupOldEntries() {
        progressMap.entrySet().removeIf(entry -> {
            ProgressInfo progress = entry.getValue();
            // Remove entries older than 2 hours
            return progress.getLastUpdateTime().isBefore(
                progress.getLastUpdateTime().minusHours(2));
        });
    }

    public void shutdown() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}