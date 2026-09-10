package dev.sanket.contactservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Strongly-typed binding for the {@code app.*} namespace in application.yml.
 * Secrets and environment-specific values are injected via environment variables
 * — never hardcoded here.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Cors cors,
        Contact contact
) {

    public record Cors(
            List<String> allowedOrigins
    ) {}

    public record Contact(
            String recipientEmail
    ) {}
}
