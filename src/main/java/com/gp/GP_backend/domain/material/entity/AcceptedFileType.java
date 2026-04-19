package com.gp.GP_backend.domain.material.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;


@Getter
@RequiredArgsConstructor
public enum AcceptedFileType {

    PDF("application/pdf", ".pdf"),
    DOC("application/msword", ".doc"),
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx"),
    TXT("text/plain", ".txt"),
    MD("text/markdown", ".md");

    // ─── Add new accepted types above this line ────────────────────────────────


    private final String mimeType;

    private final String extension;

    public static Optional<AcceptedFileType> fromMimeType(String mimeType) {
        if (mimeType == null)
            return Optional.empty();
        String normalized = mimeType.split(";")[0].trim().toLowerCase();
        return Arrays.stream(values())
                .filter(t -> t.mimeType.equalsIgnoreCase(normalized))
                .findFirst();
    }

    public static boolean isAccepted(String mimeType) {
        return fromMimeType(mimeType).isPresent();
    }

    // human-readable list of accepted MIME types for error messages
    public static String acceptedMimeTypes() {
        return Arrays.stream(values())
                .map(AcceptedFileType::getMimeType)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }
}