package com.quikko.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;

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