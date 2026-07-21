package com.quikko.model.dto;

public class CapsuleLeaveRequest {

    private String anonId;
    private String pairId;
    private String message;

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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
