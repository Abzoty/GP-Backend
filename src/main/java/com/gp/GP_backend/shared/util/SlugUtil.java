package com.gp.GP_backend.shared.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utility for converting arbitrary strings into URL-safe slugs.
 *
 * <p>
 * Example:
 * 
 * <pre>
 * SlugUtil.toSlug("Data Structures & Algorithms!")
 * // → "data-structures-algorithms"
 * </pre>
 *
 * <p>
 * Used by {@link com.gp.GP_backend.domain.space.service.SpaceService}
 * when creating a new space to populate the {@code slug} field.
 */
public final class SlugUtil {

    /** Matches any character that is not a lowercase letter, digit, or hyphen. */
    private static final Pattern NON_SLUG_CHAR = Pattern.compile("[^a-z0-9-]");

    /** Collapses consecutive hyphens into one. */
    private static final Pattern CONSECUTIVE_HYPHENS = Pattern.compile("-{2,}");

    // Utility class — no instantiation
    private SlugUtil() {
    }

    /**
     * Converts a display name into a lowercase, hyphen-separated slug.
     *
     * <p>
     * Steps:
     * <ol>
     * <li>Normalise Unicode (NFD) to separate base characters from diacritics.</li>
     * <li>Strip diacritics and non-ASCII characters.</li>
     * <li>Lowercase.</li>
     * <li>Replace whitespace/underscores with hyphens.</li>
     * <li>Remove any remaining non-slug characters.</li>
     * <li>Collapse consecutive hyphens and trim leading/trailing hyphens.</li>
     * </ol>
     *
     * @param input the raw display name (e.g. space name).
     * @return a URL-safe slug string; empty string if input produces no valid
     *         chars.
     */
    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);

        return normalized
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "") // strip diacritics
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\s_]+", "-") // whitespace → hyphen
                .replaceAll(NON_SLUG_CHAR.pattern(), "") // remove non-slug chars
                .replaceAll(CONSECUTIVE_HYPHENS.pattern(), "-") // collapse hyphens
                .replaceAll("^-|-$", ""); // trim edge hyphens
    }

    /**
     * Appends a numeric suffix to make a slug unique when a collision exists.
     *
     * <p>
     * Example: {@code "data-structures"} → {@code "data-structures-2"}
     *
     * @param base   the original slug.
     * @param suffix a positive integer suffix.
     * @return the suffixed slug.
     */
    public static String withSuffix(String base, int suffix) {
        return base + "-" + suffix;
    }
}
