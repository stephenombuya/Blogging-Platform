package com.blogplatform.util;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class SlugUtil {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern MULTI_DASH  = Pattern.compile("-+");

    public String toSlug(String input) {
        if (input == null || input.isBlank()) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String slug = normalized
                .toLowerCase(Locale.ENGLISH)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        slug = WHITESPACE.matcher(slug).replaceAll("-");
        slug = NON_LATIN.matcher(slug).replaceAll("");
        slug = MULTI_DASH.matcher(slug).replaceAll("-");
        return slug.strip().replaceAll("^-|-$", "");
    }

    /**
     * Makes a slug unique by appending a numeric suffix if needed.
     */
    public String makeUnique(String baseSlug, java.util.function.Predicate<String> existsFn) {
        String candidate = baseSlug;
        int counter = 1;
        while (existsFn.test(candidate)) {
            candidate = baseSlug + "-" + counter++;
        }
        return candidate;
    }
}
