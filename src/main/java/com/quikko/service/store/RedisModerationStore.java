package com.quikko.service.store;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConditionalOnProperty(prefix = "quikko.matching", name = "queue-store", havingValue = "redis")
public class RedisModerationStore implements ModerationStore {

    private static final String REPORTS_PREFIX = "quikko:reports:";
    private static final String BAN_PREFIX = "quikko:ban:";
    private static final String JOINS_PREFIX = "quikko:joins:";

    private final StringRedisTemplate redis;

    @Autowired
    public RedisModerationStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public int recordReport(String ip) {
        if (ip == null) {
            return 0;
        }
        Long count = redis.opsForValue().increment(REPORTS_PREFIX + ip);
        return count == null ? 0 : count.intValue();
    }

    @Override
    public void ban(String ip, Duration duration) {
        if (ip == null) {
            return;
        }
        redis.opsForValue().set(BAN_PREFIX + ip, "1", duration);
    }

    @Override
    public boolean isBanned(String ip) {
        if (ip == null) {
            return false;
        }
        return Boolean.TRUE.equals(redis.hasKey(BAN_PREFIX + ip));
    }

    @Override
    public int incrementJoinAttempts(String key, Duration window) {
        String redisKey = JOINS_PREFIX + key;
        Long count = redis.opsForValue().increment(redisKey);
        if (count != null && count == 1L) {
            redis.expire(redisKey, window);
        }
        return count == null ? 0 : count.intValue();
    }
}
