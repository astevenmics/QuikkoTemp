package com.quikko.controller;

import com.quikko.config.AppProperties;
import com.quikko.model.dto.IceServer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Small read-only config endpoints the frontend fetches on load: suggested
 * interest tags (Common Ground) and the WebRTC ICE server list (STUN always,
 * TURN only if configured via environment variables).
 */
@RestController
public class ConfigController {

    private static final String HMAC_ALGORITHM = "HmacSHA1";

    private final AppProperties props;

    public ConfigController(AppProperties props) {
        this.props = props;
    }

    @GetMapping("/api/interests")
    public Map<String, List<String>> interests() {
        return Map.of("suggested", props.getInterests().suggestedList());
    }

    @GetMapping("/api/webrtc/ice-servers")
    public Map<String, List<IceServer>> iceServers() {
        List<IceServer> servers = new ArrayList<>();
        servers.add(new IceServer(props.getWebrtc().stunList(), null, null));
        String turnUrl = props.getWebrtc().getTurnUrl();
        String turnSecret = props.getWebrtc().getTurnSecret();
        if (turnUrl != null && !turnUrl.isBlank() && turnSecret != null && !turnSecret.isBlank()) {
            servers.add(ephemeralTurnServer(turnUrl, turnSecret));
        }
        return Map.of("iceServers", servers);
    }

    /**
     * Standard coturn "time-limited REST API" / use-auth-secret scheme:
     * username is an expiry timestamp (plus a label), credential is
     * base64(HMAC-SHA1(secret, username)). The server never hands out the
     * secret itself or a long-lived static credential — a caller only ever
     * gets a username/credential pair that stops working after
     * {@code turn-credential-ttl-seconds}.
     */
    private IceServer ephemeralTurnServer(String turnUrl, String turnSecret) {
        long expiry = Instant.now().plusSeconds(props.getWebrtc().getTurnCredentialTtlSeconds()).getEpochSecond();
        String username = expiry + ":quikko";
        String credential = hmacSha1Base64(turnSecret, username);
        return new IceServer(List.of(turnUrl), username, credential);
    }

    private String hmacSha1Base64(String secret, String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(raw);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to compute TURN credential", e);
        }
    }
}
