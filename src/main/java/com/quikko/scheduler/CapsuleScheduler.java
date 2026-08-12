package com.quikko.scheduler;

import com.quikko.model.Capsule;
import com.quikko.repository.CapsuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Daily job that finds pairings where BOTH sides left a Time Capsule for each other and the 7-day unlock window has passed, and marks them delivered.
 * (The claim page in CapsuleService also computes this live on demand, so a user isn't stuck waiting for the nightly run.
 * This job exists per the double-opt-in unlock design and is the natural place to hook in a push/email notification if Quikko ever grows accounts.)
 */
@Component
public class CapsuleScheduler {

    private static final Logger log = LoggerFactory.getLogger(CapsuleScheduler.class);

    private final CapsuleRepository repository;

    public CapsuleScheduler(CapsuleRepository repository) {
        this.repository = repository;
    }

    @Scheduled(cron = "${quikko.capsule.unlock-check-cron:0 0 3 * * *}")
    @Transactional
    public void unlockEligibleCapsules() {
        List<Capsule> undelivered = repository.findByDeliveredFalse();
        if (undelivered.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        Set<String> processedPairIds = new HashSet<>();
        int unlockedCount = 0;

        for (Capsule capsule : undelivered) {
            if (!processedPairIds.add(capsule.getPairId() + "|" + capsule.getSenderAnonId() + "|" + capsule.getRecipientAnonId())) {
                continue;
            }
            Optional<Capsule> reciprocalOpt = repository.findBySenderAnonIdAndRecipientAnonIdAndPairId(
                    capsule.getRecipientAnonId(), capsule.getSenderAnonId(), capsule.getPairId());
            if (reciprocalOpt.isEmpty()) {
                continue; // one-sided: silently never delivered
            }
            Capsule reciprocal = reciprocalOpt.get();
            boolean bothPastUnlock = !now.isBefore(capsule.getUnlockAt()) && !now.isBefore(reciprocal.getUnlockAt());
            if (!bothPastUnlock) {
                continue;
            }
            if (!capsule.isDelivered()) {
                capsule.setDelivered(true);
                repository.save(capsule);
                unlockedCount++;
            }
            if (!reciprocal.isDelivered()) {
                reciprocal.setDelivered(true);
                repository.save(reciprocal);
                unlockedCount++;
            }
        }
        if (unlockedCount > 0) {
            log.info("Time Capsule scheduler unlocked {} capsule(s)", unlockedCount);
        }
    }
}
