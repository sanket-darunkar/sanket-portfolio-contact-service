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
        Resend resend,
        Contact contact
) {

    public record Cors(
            List<String> allowedOrigins
    ) {}

    /**
     * Resend HTTP Email API configuration.
     * <ul>
     *   <li>{@code apiKey}   — injected from {@code RESEND_API_KEY} env var</li>
     *   <li>{@code fromEmail} — injected from {@code RESEND_FROM_EMAIL} env var</li>
     * </ul>
     */
    public record Resend(
            String apiKey,
            String fromEmail
    ) {}

    public record Contact(
            String recipientEmail
    ) {}
}
