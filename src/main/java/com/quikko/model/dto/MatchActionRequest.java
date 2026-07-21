package com.quikko.model.dto;

/**
 * Generic {anonId, pairId} envelope used for skip / stop / report actions.
 */
public class MatchActionRequest {

    private String anonId;
    private String pairId;

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
}
