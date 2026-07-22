package com.quikko.model.dto;

import java.util.List;

public class QueueJoinRequest {

    private String anonId;
    private List<String> interests;
    private String captchaId;
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

    public String getCaptchaId() {
        return captchaId;
    }

    public void setCaptchaId(String captchaId) {
        this.captchaId = captchaId;
    }

    public Boolean getVideoEnabled() {
        return videoEnabled;
    }

    public void setVideoEnabled(Boolean videoEnabled) {
        this.videoEnabled = videoEnabled;
    }
}
