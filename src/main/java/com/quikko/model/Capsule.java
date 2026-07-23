package com.quikko.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A "Time Capsule": an optional note one anonymous user leaves for the
 * stranger they were just paired with. Capsules only ever unlock/deliver if
 * BOTH sides of a pairing independently left one for each other and the
 * 7-day unlock window has passed (see CapsuleService / CapsuleScheduler).
 */
@Entity
@Table(name = "capsules", indexes = {
        @Index(name = "idx_capsule_token", columnList = "token", unique = true),
        @Index(name = "idx_capsule_pair", columnList = "pairId"),
        @Index(name = "idx_capsule_recipient", columnList = "recipientAnonId")
})
public class Capsule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Column(nullable = false, length = 64)
    private String pairId;

    @Column(nullable = false, length = 64)
    private String senderAnonId;

    @Column(nullable = false, length = 64)
    private String recipientAnonId;

    @Column(nullable = false, length = 280)
    private String message;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant unlockAt;

    @Column(nullable = false)
    private boolean delivered = false;

    protected Capsule() {
        // JPA
    }

    public Capsule(String token, String pairId, String senderAnonId, String recipientAnonId,
                   String message, Instant createdAt, Instant unlockAt) {
        this.token = token;
        this.pairId = pairId;
        this.senderAnonId = senderAnonId;
        this.recipientAnonId = recipientAnonId;
        this.message = message;
        this.createdAt = createdAt;
        this.unlockAt = unlockAt;
    }

    public UUID getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public String getPairId() {
        return pairId;
    }

    public String getSenderAnonId() {
        return senderAnonId;
    }

    public String getRecipientAnonId() {
        return recipientAnonId;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUnlockAt() {
        return unlockAt;
    }

    public boolean isDelivered() {
        return delivered;
    }

    public void setDelivered(boolean delivered) {
        this.delivered = delivered;
    }
}
