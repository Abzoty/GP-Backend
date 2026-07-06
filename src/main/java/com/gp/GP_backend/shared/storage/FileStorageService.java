package com.gp.GP_backend.shared.storage;

import com.gp.GP_backend.domain.material.entity.AcceptedFileType;
import com.gp.GP_backend.shared.exception.ApiException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.UUID;


@Service
@Slf4j
public class FileStorageService {

    // ─── Global size limit ────────────────────────────────────────────────────
    public static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB

    // ─── Configuration ────────────────────────────────────────────────────────

    @Value("${app.storage.upload-dir:uploads}")
    private String uploadDirPath;

    private Path uploadDir;

    @PostConstruct
    public void init() {
        uploadDir = Paths.get(uploadDirPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadDir);
            log.info("File storage initialized at: {}", uploadDir);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not create upload directory: " + uploadDir, e);
        }
    }

    // ─── Store ────────────────────────────────────────────────────────────────

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Uploaded file must not be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            long maxMb = MAX_FILE_SIZE_BYTES / (1024 * 1024);
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "File size exceeds the maximum allowed limit of " + maxMb + " MB");
        }

        AcceptedFileType fileType = AcceptedFileType.fromMimeType(file.getContentType())
                .orElseThrow(() -> new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "File type '" + file.getContentType() + "' is not supported. "
                                + "Accepted types: " + AcceptedFileType.acceptedMimeTypes()));

        // Use a random UUID as the filename to prevent collisions and path-traversal
        String filename = UUID.randomUUID() + fileType.getExtension();

        try {
            Path target = uploadDir.resolve(filename).normalize();
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Stored uploaded file '{}' ({} bytes)", filename, file.getSize());
            return filename;
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to store file. Please try again.", e);
        }
    }

    // ─── Load ─────────────────────────────────────────────────────────────────

    public Resource loadAsResource(String filename) {
        try {
            Path filePath = uploadDir.resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ApiException(HttpStatus.NOT_FOUND,
                        "File not found on server: " + filename);
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not resolve file path for: " + filename);
        }
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    public void delete(String filename) {
        if (filename == null || filename.isBlank())
            return;
        try {
            Path filePath = uploadDir.resolve(filename).normalize();
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.debug("Deleted file '{}' from storage", filename);
            } else {
                log.warn("File '{}' was not found on disk during deletion — skipping", filename);
            }
        } catch (IOException e) {
            log.warn("Could not delete file '{}': {}", filename, e.getMessage());
        }
    }
}