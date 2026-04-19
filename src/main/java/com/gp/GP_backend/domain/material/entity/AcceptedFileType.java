package com.gp.GP_backend.domain.material.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

/**
 * Enumerates every file type accepted for material uploads.
 *
 * <p>
 * <b>To add a new type:</b> add a constant with the correct MIME type and
 * extension.
 * <b>To remove one:</b> delete the constant.
 * No other class needs to be changed — the enum drives validation, storage, and
 * HTTP response headers automatically.
 *
 * <p>
 * The enum {@code name()} (e.g. {@code "PDF"}) is what gets persisted in
 * {@code Material.resourceType} for file-based materials.
 */
@Getter
@RequiredArgsConstructor
public enum AcceptedFileType {

    PDF("application/pdf", ".pdf"),
    DOC("application/msword", ".doc"),
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx"),
    TXT("text/plain", ".txt"),
    MD("text/markdown", ".md");

    // ─── Add new accepted types above this line ────────────────────────────────

    /**
     * MIME type as reported by the browser in the multipart Content-Type header.
     */
    private final String mimeType;

    /** File extension including the leading dot (e.g. {@code ".pdf"}). */
    private final String extension;

    /**
     * Looks up an {@link AcceptedFileType} by MIME type, ignoring charset suffixes
     * (e.g. {@code "text/plain; charset=UTF-8"} → {@code TXT}).
     *
     * @param mimeType raw Content-Type header value; may be null.
     * @return the matching type, or empty if none matches.
     */
    public static Optional<AcceptedFileType> fromMimeType(String mimeType) {
        if (mimeType == null)
            return Optional.empty();
        String normalized = mimeType.split(";")[0].trim().toLowerCase();
        return Arrays.stream(values())
                .filter(t -> t.mimeType.equalsIgnoreCase(normalized))
                .findFirst();
    }

    /** @return {@code true} if the given MIME type appears in the accepted list. */
    public static boolean isAccepted(String mimeType) {
        return fromMimeType(mimeType).isPresent();
    }

    /**
     * @return a human-readable comma-separated list of accepted MIME types,
     *         suitable for error messages.
     */
    public static String acceptedMimeTypes() {
        return Arrays.stream(values())
                .map(AcceptedFileType::getMimeType)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }
}