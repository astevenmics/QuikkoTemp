package com.quikko.service;

import com.quikko.config.AppProperties;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Simple, configurable word-list profanity filter for chat text.
 */
@Service
public class ProfanityFilterService {

    private final Set<String> profanity;
    private final Pattern pattern;

    public ProfanityFilterService(AppProperties props) {
        this.profanity = props.getModeration().profanitySet();
        this.pattern = profanity.isEmpty() ? null : Pattern.compile(
                "\\b(" + String.join("|", profanity.stream().map(Pattern::quote).toList()) + ")\\b",
                Pattern.CASE_INSENSITIVE);
    }

    public boolean containsProfanity(String text) {
        if (text == null || pattern == null) {
            return false;
        }
        return pattern.matcher(text).find();
    }

    /**
     * Replaces every profane word with asterisks of the same length.
     */
    public String filter(String text) {
        if (text == null || pattern == null) {
            return text;
        }
        return pattern.matcher(text).replaceAll(match -> "*".repeat(match.group().length()));
    }
}
