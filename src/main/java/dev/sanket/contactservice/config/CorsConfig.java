package dev.sanket.contactservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * CORS configuration driven by {@link AppProperties}.
 *
 * <p>Allowed origins are supplied via the {@code CORS_ALLOWED_ORIGINS} environment
 * variable (comma-separated) or via the {@code app.cors.allowed-origins} YAML
 * property.  The value is normalised here — each origin is trimmed and any
 * accidental trailing slash is stripped — so origin comparisons are exact.</p>
 *
 * <p>Never use {@code allowedOrigins("*")}; explicit origins are required so that
 * pre-flight and credentialed requests are handled correctly.</p>
 */
@Configuration
public class CorsConfig {

    private static final Logger log = LoggerFactory.getLogger(CorsConfig.class);

    private final AppProperties appProperties;

    public CorsConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Bean
    public CorsFilter corsFilter() {
        // Spring's relaxed binding may deliver the comma-separated env-var value as a
        // single-element list (the whole string) when the placeholder is resolved inline
        // in YAML.  We therefore re-split every element on commas ourselves so the filter
        // always ends up with one origin per list entry, regardless of how the value was
        // supplied (YAML list, comma-separated env var, etc.).
        List<String> allowedOrigins = appProperties.cors().allowedOrigins().stream()
                .flatMap(entry -> java.util.Arrays.stream(entry.split(",")))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                // Strip any accidental trailing slash — browsers never send one in the
                // Origin header, so "https://example.com/" would never match.
                .map(s -> s.endsWith("/") ? s.substring(0, s.length() - 1) : s)
                .distinct()
                .toList();

        log.info("Configuring CORS — allowed origins: {}", allowedOrigins);

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        // Allow the headers that browsers send on pre-flight and actual requests.
        config.setAllowedHeaders(List.of("Content-Type", "Accept", "Origin",
                "Access-Control-Request-Method", "Access-Control-Request-Headers"));
        config.setAllowCredentials(false);
        // Cache pre-flight response for 1 hour to reduce OPTIONS round-trips.
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);

        return new CorsFilter(source);
    }
}
