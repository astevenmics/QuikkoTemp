package com.quikko.repository;

import com.quikko.model.Capsule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CapsuleRepository extends JpaRepository<Capsule, UUID> {

    Optional<Capsule> findByToken(String token);

    Optional<Capsule> findBySenderAnonIdAndRecipientAnonIdAndPairId(String senderAnonId, String recipientAnonId, String pairId);

    List<Capsule> findByDeliveredFalse();
}
