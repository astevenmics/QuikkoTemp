package com.quikko.model.dto;

import java.util.List;

public class QueueJoinRequest {

    private String anonId;
    private List<String> interests;
    private Boolean videoEnabled;

    public String getAnonId() {
        return anonId;
    }

    public void setAnonId(String anonId) {
        this.anonId = anonId;
    }

    public List<String> getInterests() {
        return interests;
    }

    public void setInterests(List<String> interests) {
        this.interests = interests;
    }

    public Boolean getVideoEnabled() {
        return videoEnabled;
    }

    public void setVideoEnabled(Boolean videoEnabled) {
        this.videoEnabled = videoEnabled;
    }
}
