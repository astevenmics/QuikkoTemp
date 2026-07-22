package com.quikko.model.dto;

import java.util.List;

/**
 * Generic outbound envelope pushed to a client's private topic
 * ({@code /topic/session/{anonId}}). Only the fields relevant to
 * {@link #type} are populated; the rest are left null and omitted from the
 * serialized JSON (see spring.jackson.default-property-inclusion=non_null).
 */
public class ServerEvent {

    public enum Type {
        WAITING,
        MATCHED,
        CHAT,
        SIGNAL,
        PARTNER_SKIPPED,
        PARTNER_LEFT,
        REPORT_ACK,
        CAPSULE_SAVED,
        ERROR,
        BANNED,
        RATE_LIMITED,
        CAPTCHA_FAILED
    }

    private Type type;
    private String pairId;
    private String text;
    private String signalType;
    private Object payload;
    private String icebreaker;
    private List<String> sharedInterests;
    private List<String> partnerInterests;
    private Boolean initiator;
    private Boolean partnerVideoEnabled;
    private String token;
    private String message;

    public static ServerEvent of(Type type) {
        ServerEvent e = new ServerEvent();
        e.type = type;
        return e;
    }

    public ServerEvent pairId(String pairId) {
        this.pairId = pairId;
        return this;
    }

    public ServerEvent text(String text) {
        this.text = text;
        return this;
    }

    public ServerEvent signalType(String signalType) {
        this.signalType = signalType;
        return this;
    }

    public ServerEvent payload(Object payload) {
        this.payload = payload;
        return this;
    }

    public ServerEvent icebreaker(String icebreaker) {
        this.icebreaker = icebreaker;
        return this;
    }

    public ServerEvent sharedInterests(List<String> sharedInterests) {
        this.sharedInterests = sharedInterests;
        return this;
    }

    public ServerEvent partnerInterests(List<String> partnerInterests) {
        this.partnerInterests = partnerInterests;
        return this;
    }

    public ServerEvent initiator(Boolean initiator) {
        this.initiator = initiator;
        return this;
    }

    public ServerEvent partnerVideoEnabled(Boolean partnerVideoEnabled) {
        this.partnerVideoEnabled = partnerVideoEnabled;
        return this;
    }

    public ServerEvent token(String token) {
        this.token = token;
        return this;
    }

    public ServerEvent message(String message) {
        this.message = message;
        return this;
    }

    public Type getType() {
        return type;
    }

    public String getPairId() {
        return pairId;
    }

    public String getText() {
        return text;
    }

    public String getSignalType() {
        return signalType;
    }

    public Object getPayload() {
        return payload;
    }

    public String getIcebreaker() {
        return icebreaker;
    }

    public List<String> getSharedInterests() {
        return sharedInterests;
    }

    public List<String> getPartnerInterests() {
        return partnerInterests;
    }

    public Boolean getInitiator() {
        return initiator;
    }

    public Boolean getPartnerVideoEnabled() {
        return partnerVideoEnabled;
    }

    public String getToken() {
        return token;
    }

    public String getMessage() {
        return message;
    }
}
