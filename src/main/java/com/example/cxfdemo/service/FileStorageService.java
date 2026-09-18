package com.example.cxfdemo.service;

import com.example.cxfdemo.model.FileUploadRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class FileStorageService implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private static final Pattern INVALID_CHARS = Pattern.compile("[^a-zA-Z0-9.-]");
    
    @Value("${file.upload.dir:/tmp/uploads}")
    private String uploadDir = "/tmp/uploads";

    @Value("${file.upload.max-size:10485760}")
    private long maxBytes = 10485760L;

    private Path storageRoot;

    public FileStorageService() {
        this.storageRoot = Paths.get(this.uploadDir).toAbsolutePath().normalize();
    }

    public FileStorageService(String uploadDir, long maxBytes) {
        this.uploadDir = uploadDir;
        this.maxBytes = maxBytes;
        this.storageRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public void afterPropertiesSet() {
        if (uploadDir != null) {
            this.storageRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
            log.info("FileStorageService initialized with storageRoot: {}", this.storageRoot);
        }
    }

    public FileUploadRecord store(InputStream input, String originalName,
                                  String contentType, String description) throws IOException {
        log.info("FileStorageService.store called for originalName: {}, maxBytes: {}", originalName, maxBytes);
        Files.createDirectories(storageRoot);
        String safeName = safeFilename(originalName);
        String storedName = UUID.randomUUID() + "-" + safeName;
        Path target = storageRoot.resolve(storedName).normalize();
        if (!target.startsWith(storageRoot)) {
            log.error("Invalid file path target: {}", target);
            throw new IllegalArgumentException("不合法的檔案路徑");
        }

        long size = 0;
        try (OutputStream output = Files.newOutputStream(target,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            byte[] buffer = new byte[16 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                size += read;
                if (size > maxBytes) {
                    log.error("File size {} exceeded maxBytes {}", size, maxBytes);
                    throw new IllegalArgumentException("檔案不可超過 " + maxBytes + " bytes");
                }
                output.write(buffer, 0, read);
            }
        } catch (IOException | RuntimeException ex) {
            log.error("FileStorageService.store error occurred while writing file: {}", storedName, ex);
            try {
                Files.deleteIfExists(target);
            } catch (IOException ignored) {
            }
            throw ex;
        }

        log.info("FileStorageService.store success for storedName: {}, size: {} bytes", storedName, size);
        FileUploadRecord record = new FileUploadRecord();
        record.setOriginalName(originalName);
        record.setStoredName(storedName);
        record.setContentType(contentType);
        record.setSizeBytes(size);
        record.setDescription(description);
        record.setCreatedAt(new Date());
        return record;
    }

    private String safeFilename(String name) {
        if (name == null || name.trim().isEmpty()) return "unknown";
        int lastSlash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (lastSlash >= 0) {
            name = name.substring(lastSlash + 1);
        }
        name = INVALID_CHARS.matcher(name).replaceAll("_");
        if (name.length() > 100) {
            name = name.substring(name.length() - 100);
        }
        return name;
    }
}
