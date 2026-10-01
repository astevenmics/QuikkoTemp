package com.quikko.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;

/**
 * Fails startup immediately, with a clear and specific message, if the
 * {@code mysql} profile is active but DB_USERNAME/DB_PASSWORD were never
 * actually set. This runs as an {@link EnvironmentPostProcessor} (registered
 * in {@code META-INF/spring.factories}) rather than as a regular bean,
 * because it needs to run before the DataSource/EntityManagerFactory beans
 * are created — those connect eagerly during startup and would otherwise
 * fail first with a confusing driver-level "communications failure" that
 * gives no hint the real problem is a missing credential (or, against a
 * real reachable server, might silently attempt an unauthenticated/
 * anonymous connection instead of failing at all).
 */
public class MysqlCredentialsCheck implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.acceptsProfiles(Profiles.of("mysql"))) {
            return;
        }
        String username = environment.getProperty("DB_USERNAME");
        String password = environment.getProperty("DB_PASSWORD");
        if (isBlank(username) || isBlank(password)) {
            throw new IllegalStateException(
                    "DB_USERNAME and DB_PASSWORD must both be set as environment variables when the "
                            + "'mysql' profile is active -- there is no default credential.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
