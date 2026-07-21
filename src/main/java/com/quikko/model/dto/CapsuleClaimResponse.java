package com.quikko.model.dto;

public class CapsuleClaimResponse {

    public enum Status {
        PENDING,   // reciprocal capsule not left yet, or unlock time not reached
        EXPIRED,   // only one side left a capsule -> never delivered
        UNLOCKED,  // both sides left one, 7 days passed -> message included
        NOT_FOUND
    }

    private Status status;
    private String message;
    private String unlockAt;

    public static CapsuleClaimResponse of(Status status) {
        CapsuleClaimResponse r = new CapsuleClaimResponse();
        r.status = status;
        return r;
    }

    public CapsuleClaimResponse message(String message) {
        this.message = message;
        return this;
    }

    public CapsuleClaimResponse unlockAt(String unlockAt) {
        this.unlockAt = unlockAt;
        return this;
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getUnlockAt() {
        return unlockAt;
    }
}
