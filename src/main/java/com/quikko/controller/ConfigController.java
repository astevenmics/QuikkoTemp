package com.quikko.controller;

import com.quikko.config.AppProperties;
import com.quikko.model.dto.IceServer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Small read-only config endpoints the frontend fetches on load: suggested
 * interest tags (Common Ground) and the WebRTC ICE server list (STUN always,
 * TURN only if configured via environment variables).
 */
@RestController
public class ConfigController {

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
        if (turnUrl != null && !turnUrl.isBlank()) {
            servers.add(new IceServer(List.of(turnUrl),
                    props.getWebrtc().getTurnUsername(),
                    props.getWebrtc().getTurnCredential()));
        }
        return Map.of("iceServers", servers);
    }
}
