package com.quikko.validation;

import java.util.Set;
import java.util.regex.Pattern;

public final class InputValidator {

    private static final Pattern ANON_ID = Pattern.compile("^[A-Za-z0-9-]{1,64}$");

    private static final Pattern PAIR_ID = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private static final Pattern INTEREST = Pattern.compile("^[\\p{L}\\p{N} .,'&/+-]{1,40}$");

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