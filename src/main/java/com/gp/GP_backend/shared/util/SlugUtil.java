package com.gp.GP_backend.shared.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;


public final class SlugUtil {

    private static final Pattern NON_SLUG_CHAR = Pattern.compile("[^a-z0-9-]");
    private static final Pattern CONSECUTIVE_HYPHENS = Pattern.compile("-{2,}");

    private SlugUtil() {
    }


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


    public static String withSuffix(String base, int suffix) {
        return base + "-" + suffix;
    }
}
