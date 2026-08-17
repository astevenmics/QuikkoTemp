package com.quikko.validation;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Strict, allow-list validation for every piece of user-supplied data that
 * enters Quikko over the WebSocket/STOMP API or the REST claim endpoint.
 * Nothing here does any escaping/sanitization of HTML or SQL — the app has
 * no server-side HTML templating (the frontend renders all user text via
 * {@code textContent}, never raw HTML) and no raw/native SQL anywhere
 * (Spring Data JPA derived queries only, always parameterized), so there is
 * no injection sink for these values to be "cleaned" for. This class exists
 * purely to reject malformed/oversized/out-of-range input at the door,
 * before it reaches any service or repository.
 */
public final class InputValidator {

    // Client-generated anonymous session id: either crypto.randomUUID()
    // (lowercase hex + hyphens) or the no-crypto fallback "id-" + base36 +
    // timestamp. Bounded length, restricted charset either way.
    private static final Pattern ANON_ID = Pattern.compile("^[A-Za-z0-9-]{1,64}$");

    // Server-generated via UUID.randomUUID().toString().
    private static final Pattern PAIR_ID = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    // Interest tag: Unicode letters/digits, spaces, and a small set of
    // punctuation that shows up in real interests ("Sci-Fi", "R&B", "C++",
    // "Women's football"). Excludes markup/quoting characters outright.
    private static final Pattern INTEREST = Pattern.compile("^[\\p{L}\\p{N} .,'&/+-]{1,40}$");

    // Server-generated via Base64.getUrlEncoder().withoutPadding() over 18
    // random bytes (24 chars), but keep a little slack either side.
    private static final Pattern CAPSULE_TOKEN = Pattern.compile("^[A-Za-z0-9_-]{16,64}$");

    private static final Set<String> SIGNAL_TYPES = Set.of("offer", "answer", "ice-candidate");

    private InputValidator() {
    }

    public static boolean isValidAnonId(String value) {
        return value != null && ANON_ID.matcher(value).matches();
    }

    public static boolean isValidPairId(String value) {
        return value != null && PAIR_ID.matcher(value).matches();
    }

    public static boolean isValidInterest(String value) {
        return value != null && INTEREST.matcher(value).matches();
    }

    public static boolean isValidSignalType(String value) {
        return value != null && SIGNAL_TYPES.contains(value);
    }

    public static boolean isValidCapsuleToken(String value) {
        return value != null && CAPSULE_TOKEN.matcher(value.trim()).matches();
    }

    /**
     * Plain single-purpose text fields (chat messages, capsule notes):
     * non-blank after trimming, within {@code maxLength}, and free of
     * control characters (a modified client could otherwise smuggle
     * arbitrary bytes — null bytes, escape sequences — into logs or
     * downstream consumers). Ordinary Unicode text, including emoji and
     * non-Latin scripts, is always allowed.
     */
    public static boolean isValidText(String value, int maxLength) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.length() > maxLength) {
            return false;
        }
        return !containsControlCharacters(trimmed);
    }

    private static boolean containsControlCharacters(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isISOControl(c) && c != '\n' && c != '\t') {
                return true;
            }
        }
        return false;
    }
}
