package org.jazz.jazzflix.controller;

import org.jazz.jazzflix.dto.ProgressInfo;
import org.jazz.jazzflix.service.ProgressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/progress")
public class ProgressSSEController {

    @Autowired
    private ProgressService progressService;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    @GetMapping(value = "/sse/{uploadId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamProgress(@PathVariable String uploadId) {
        SseEmitter emitter = new SseEmitter(0L); // No timeout

        executor.execute(() -> {
            try {
                while (true) {
                    ProgressInfo progress = progressService.getProgress(uploadId);
                    if (progress != null) {
                        emitter.send(SseEmitter.event().data(progress));
                    }

                    // Check if completed or failed
                    if (progress != null && (progress.getStatus().equals("COMPLETED") || progress.getStatus().equals("FAILED"))) {
                        emitter.complete();
                        break;
                    }

                    Thread.sleep(1000); // Poll every second
                }
            } catch (IOException | InterruptedException e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}