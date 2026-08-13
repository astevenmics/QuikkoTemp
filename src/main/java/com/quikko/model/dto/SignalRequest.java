package com.quikko.model.dto;

/**
 * WebRTC signaling envelope relayed verbatim between the two peers of a pair. signalType is one of "offer", "answer", "ice-candidate".
 */
public class SignalRequest {

    private String anonId;
    private String pairId;
    private String signalType;
    private Object payload;

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

    public String getSignalType() {
        return signalType;
    }

    public void setSignalType(String signalType) {
        this.signalType = signalType;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }
}
