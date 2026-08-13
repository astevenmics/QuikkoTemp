package com.quikko.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * Hand-rolled Redis connection beans, created only when
 * {@code quikko.matching.queue-store=redis}.
 *
 * Spring Boot's own RedisAutoConfiguration is excluded unconditionally (see
 * application.properties) so the default in-memory profile never attempts a
 * connection to Redis at startup — Lettuce eagerly opens a shared connection
 * as soon as its ConnectionFactory bean is created, which would otherwise
 * fail fast when no Redis is running.
 *
 * Gating these beans on the single quikko.matching.queue-store property
 * (rather than requiring a separate "redis" Spring profile to also be
 * active) means setting that one property is always sufficient on its own —
 * no risk of ending up with RedisMatchQueueStore/RedisModerationStore
 * enabled but no StringRedisTemplate bean to autowire.
 */
@Configuration
@EnableConfigurationProperties(RedisProperties.class)
@ConditionalOnProperty(prefix = "quikko.matching", name = "queue-store", havingValue = "redis")
public class RedisConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory(RedisProperties props) {
        RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration(props.getHost(), props.getPort());
        if (props.getPassword() != null && !props.getPassword().isBlank()) {
            standalone.setPassword(props.getPassword());
        }
        Duration timeout = props.getTimeout() != null ? props.getTimeout() : Duration.ofSeconds(2);
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(timeout)
                .build();
        return new LettuceConnectionFactory(standalone, clientConfig);
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
