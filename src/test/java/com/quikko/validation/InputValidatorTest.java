package com.quikko.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InputValidatorTest {

    @Test
    void anonId_acceptsUuidShapedAndFallbackShapedIds() {
        assertThat(InputValidator.isValidAnonId("a1b2c3d4-e5f6-7890-aaaa-bbbbccccdddd")).isTrue();
        assertThat(InputValidator.isValidAnonId("id-abc123def456")).isTrue();
    }

    @Test
    void anonId_rejectsScriptInjectionAttempt() {
        assertThat(InputValidator.isValidAnonId("<script>alert(1)</script>")).isFalse();
    }

    @Test
    void anonId_rejectsNullAndBlank() {
        assertThat(InputValidator.isValidAnonId(null)).isFalse();
        assertThat(InputValidator.isValidAnonId("")).isFalse();
    }

    @Test
    void pairId_acceptsOnlyUuidShape() {
        assertThat(InputValidator.isValidPairId("11111111-1111-1111-1111-111111111111")).isTrue();
        assertThat(InputValidator.isValidPairId("not-a-uuid")).isFalse();
        assertThat(InputValidator.isValidPairId(null)).isFalse();
    }

    @Test
    void interest_acceptsRealisticTagsAndRejectsMarkup() {
        assertThat(InputValidator.isValidInterest("Sci-Fi")).isTrue();
        assertThat(InputValidator.isValidInterest("R&B")).isTrue();
        assertThat(InputValidator.isValidInterest("Women's football")).isTrue();
        assertThat(InputValidator.isValidInterest("<img src=x onerror=alert(1)>")).isFalse();
    }

    @Test
    void signalType_onlyAllowsTheThreeKnownValues() {
        assertThat(InputValidator.isValidSignalType("offer")).isTrue();
        assertThat(InputValidator.isValidSignalType("answer")).isTrue();
        assertThat(InputValidator.isValidSignalType("ice-candidate")).isTrue();
        assertThat(InputValidator.isValidSignalType("javascript:alert(1)")).isFalse();
        assertThat(InputValidator.isValidSignalType(null)).isFalse();
    }

    @Test
    void text_rejectsBlankOversizedAndControlCharacters() {
        assertThat(InputValidator.isValidText(null, 100)).isFalse();
        assertThat(InputValidator.isValidText("   ", 100)).isFalse();
        assertThat(InputValidator.isValidText("x".repeat(101), 100)).isFalse();
        assertThat(InputValidator.isValidText("hello\u0007world", 100)).isFalse();
    }

    @Test
    void text_allowsOrdinaryUnicodeIncludingEmoji() {
        assertThat(InputValidator.isValidText("hey there 😀", 100)).isTrue();
    }

    @Test
    void capsuleToken_matchesGeneratedTokenShapeAndRejectsTooShort() {
        assertThat(InputValidator.isValidCapsuleToken("fK2F4psaWaPZ2yzXxXreuka9")).isTrue();
        assertThat(InputValidator.isValidCapsuleToken("short")).isFalse();
    }
}
