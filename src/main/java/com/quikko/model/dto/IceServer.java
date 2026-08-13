package com.quikko.model.dto;

import java.util.List;

/**
 * Mirrors the shape of an RTCIceServer entry expected by the browser RTCPeerConnection constructor.
 */
public class IceServer {

    private List<String> urls;
    private String username;
    private String credential;

    public IceServer(List<String> urls, String username, String credential) {
        this.urls = urls;
        this.username = username;
        this.credential = credential;
    }

    public List<String> getUrls() {
        return urls;
    }

    public String getUsername() {
        return username;
    }

    public String getCredential() {
        return credential;
    }
}
