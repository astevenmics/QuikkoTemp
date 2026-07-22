package com.quikko.controller;

import com.quikko.model.dto.CaptchaVerifyRequest;
import com.quikko.service.CaptchaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/captcha")
public class CaptchaController {

    private final CaptchaService captchaService;

    public CaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    @GetMapping("/new")
    public Map<String, Object> newChallenge() {
        if (!captchaService.isEnabled()) {
            return Map.of("enabled", false);
        }
        CaptchaService.ChallengeView challenge = captchaService.newChallenge();
        return Map.of("enabled", true, "challengeId", challenge.challengeId(), "question", challenge.question());
    }

    @PostMapping("/verify")
    public Map<String, Boolean> verify(@RequestBody CaptchaVerifyRequest req) {
        if (!captchaService.isEnabled()) {
            return Map.of("valid", true);
        }
        boolean valid = captchaService.verify(req.getChallengeId(), req.getAnswer());
        return Map.of("valid", valid);
    }
}
