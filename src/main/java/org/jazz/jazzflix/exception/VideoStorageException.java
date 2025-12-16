package org.jazz.jazzflix.exception;

public class VideoStorageException extends RuntimeException {
    public VideoStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public VideoStorageException(String message) {
        super(message);
    }
}
