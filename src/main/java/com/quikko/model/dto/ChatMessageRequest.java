package com.quikko.model.dto;

public class ChatMessageRequest {

    private String anonId;
    private String pairId;
    private String text;

    public String getAnonId() {
        return anonId;
    }

    public void setAnonId(String anonId) {
        this.anonId = anonId;
    }

    public String getPairId() {
        return pairId;
    }

    public void setPairId(String pairId) {
        this.pairId = pairId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
