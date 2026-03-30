package com.blogplatform.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SlugUtil Tests")
class SlugUtilTest {

    private final SlugUtil slugUtil = new SlugUtil();

    @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
    @CsvSource({
        "Hello World,              hello-world",
        "Spring Boot & Java,       spring-boot-java",
        "  Leading/Trailing Spaces , leading-trailing-spaces",
        "Café au lait,             cafe-au-lait",
        "Multiple   Spaces,        multiple-spaces",
        "already-a-slug,           already-a-slug",
        "UPPER CASE,               upper-case",
        "special!@#$%chars,        specialchars",
    })
    @DisplayName("toSlug: various inputs produce correct slugs")
    void toSlug(String input, String expected) {
        assertThat(slugUtil.toSlug(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("toSlug: null input returns empty string")
    void toSlug_null_returnsEmpty() {
        assertThat(slugUtil.toSlug(null)).isEqualTo("");
    }

    @Test
    @DisplayName("makeUnique: returns base slug when not taken")
    void makeUnique_baseNotTaken_returnsBase() {
        String result = slugUtil.makeUnique("my-slug", s -> false);
        assertThat(result).isEqualTo("my-slug");
    }

    @Test
    @DisplayName("makeUnique: appends counter when base is taken")
    void makeUnique_baseTaken_appendsCounter() {
        Set<String> taken = Set.of("my-slug", "my-slug-1", "my-slug-2");
        String result = slugUtil.makeUnique("my-slug", taken::contains);
        assertThat(result).isEqualTo("my-slug-3");
    }

    @Test
    @DisplayName("makeUnique: counter increments correctly")
    void makeUnique_counterIncrements() {
        AtomicInteger calls = new AtomicInteger(0);
        // return true (exists) for first 4 calls, then false
        String result = slugUtil.makeUnique("post", s -> calls.getAndIncrement() < 4);
        assertThat(result).isEqualTo("post-4");
    }
}
