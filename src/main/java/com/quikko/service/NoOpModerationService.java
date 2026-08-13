package com.quikko.service;

import org.springframework.stereotype.Service;

/**
 * Default no-op implementation. Always allows content;
 */
@Service
public class NoOpModerationService implements ModerationService {

    @Override
    public ModerationResult checkText(String anonId, String text) {
        return ModerationResult.allow();
    }

    @Override
    public ModerationResult checkVideoFrame(String anonId, byte[] frameData) {
        return ModerationResult.allow();
    }
}
