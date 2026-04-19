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

/**
 * Handles disk-based storage for uploaded material files.
 *
 * <p>
 * Files are written to the directory configured by
 * {@code app.storage.upload-dir}
 * (defaults to {@code uploads/} relative to the working directory).
 * The directory is created automatically on startup if it does not exist.
 *
 * <p>
 * <b>To change the maximum upload size:</b> update
 * {@link #MAX_FILE_SIZE_BYTES}.
 * Also keep {@code spring.servlet.multipart.max-file-size} in
 * {@code application-dev.properties} in sync — Spring's multipart limit is
 * checked
 * first (before the request reaches this service).
 *
 * <p>
 * <b>To add or remove accepted file types:</b> edit
 * {@link AcceptedFileType} only — no changes needed here.
 */
@Service
@Slf4j
public class FileStorageService {

    // ─── Global size limit ────────────────────────────────────────────────────

    /**
     * Maximum allowed upload size <strong>in bytes</strong>.
     * Change this one constant to raise or lower the limit everywhere.
     * Keep {@code spring.servlet.multipart.max-file-size} ≥ this value.
     */
    public static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB

    // ─── Configuration ────────────────────────────────────────────────────────

    @Value("${app.storage.upload-dir:uploads}")
    private String uploadDirPath;

    private Path uploadDir;

    /**
     * Resolves the upload directory path and creates it (including any missing
     * parent directories) on application startup.
     *
     * @throws IllegalStateException if the directory cannot be created.
     */
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

    /**
     * Validates and persists a multipart file to disk.
     *
     * <p>
     * Validation order:
     * <ol>
     * <li>File must not be empty.</li>
     * <li>File size must not exceed {@link #MAX_FILE_SIZE_BYTES}.</li>
     * <li>MIME type must appear in {@link AcceptedFileType}.</li>
     * </ol>
     *
     * @param file the uploaded multipart file.
     * @return the generated filename ({@code UUID + extension}) as stored on disk.
     *         Persist this in {@code Material.url}.
     * @throws ApiException 400 if the file is empty or too large.
     * @throws ApiException 415 if the MIME type is not in the accepted list.
     * @throws ApiException 500 if writing to disk fails.
     */
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

    /**
     * Loads a previously stored file as a Spring {@link Resource} for streaming
     * to the client.
     *
     * @param filename the stored filename returned by {@link #store}.
     * @return a readable resource pointing to the file on disk.
     * @throws ApiException 404 if the file does not exist on disk.
     * @throws ApiException 500 if the path cannot be resolved.
     */
    public Resource loadAsResource(String filename) {
        try {
            // normalize() prevents path-traversal attacks (e.g. "../../etc/passwd")
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

    /**
     * Deletes a stored file from disk.
     *
     * <p>
     * Silently succeeds if the file does not exist — this prevents a missing
     * file from blocking the database record deletion that follows.
     *
     * @param filename the stored filename to remove; null/blank values are ignored.
     */
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
            // Log but do not rethrow — the DB record still needs to be removed
            log.warn("Could not delete file '{}': {}", filename, e.getMessage());
        }
    }
}