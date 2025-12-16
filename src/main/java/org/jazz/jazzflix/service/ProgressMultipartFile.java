package org.jazz.jazzflix.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ProgressMultipartFile implements MultipartFile {

    private final MultipartFile originalFile;
    private final ProgressCallback callback;

    public interface ProgressCallback {
        void onProgress(long bytesRead);
    }

    public ProgressMultipartFile(MultipartFile originalFile, ProgressCallback callback) {
        this.originalFile = originalFile;
        this.callback = callback;
    }

    @Override
    public String getName() {
        return originalFile.getName();
    }

    @Override
    public String getOriginalFilename() {
        return originalFile.getOriginalFilename();
    }

    @Override
    public String getContentType() {
        return originalFile.getContentType();
    }

    @Override
    public boolean isEmpty() {
        return originalFile.isEmpty();
    }

    @Override
    public long getSize() {
        return originalFile.getSize();
    }

    @Override
    public byte[] getBytes() throws IOException {
        byte[] bytes = originalFile.getBytes();
        if (callback != null) {
            callback.onProgress(bytes.length);
        }
        return bytes;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ProgressInputStream(originalFile.getInputStream());
    }

    @Override
    public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
        originalFile.transferTo(dest);
        if (callback != null) {
            callback.onProgress(originalFile.getSize());
        }
    }

    private class ProgressInputStream extends InputStream {
        private final InputStream originalStream;
        private long totalBytesRead = 0;

        public ProgressInputStream(InputStream originalStream) {
            this.originalStream = originalStream;
        }

        @Override
        public int read() throws IOException {
            int data = originalStream.read();
            if (data != -1 && callback != null) {
                totalBytesRead++;
                callback.onProgress(totalBytesRead);
            }
            return data;
        }

        @Override
        public int read(byte[] b) throws IOException {
            int bytesRead = originalStream.read(b);
            if (bytesRead > 0 && callback != null) {
                totalBytesRead += bytesRead;
                callback.onProgress(totalBytesRead);
            }
            return bytesRead;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int bytesRead = originalStream.read(b, off, len);
            if (bytesRead > 0 && callback != null) {
                totalBytesRead += bytesRead;
                callback.onProgress(totalBytesRead);
            }
            return bytesRead;
        }

        @Override
        public long skip(long n) throws IOException {
            return originalStream.skip(n);
        }

        @Override
        public int available() throws IOException {
            return originalStream.available();
        }

        @Override
        public void close() throws IOException {
            originalStream.close();
        }

        @Override
        public synchronized void mark(int readlimit) {
            originalStream.mark(readlimit);
        }

        @Override
        public synchronized void reset() throws IOException {
            originalStream.reset();
        }

        @Override
        public boolean markSupported() {
            return originalStream.markSupported();
        }
    }
}