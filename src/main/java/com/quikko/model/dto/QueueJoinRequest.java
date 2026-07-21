package com.quikko.model.dto;

import java.util.List;

public class QueueJoinRequest {

    private String anonId;
    private List<String> interests;

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
}
