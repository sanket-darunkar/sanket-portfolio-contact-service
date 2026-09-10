package dev.sanket.contactservice.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sanket.contactservice.config.AppProperties;
import dev.sanket.contactservice.dto.ContactRequest;
import dev.sanket.contactservice.exception.ContactServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Resend HTTP Email API implementation of {@link EmailService}.
 *
 * <p>Sends a multipart (HTML + plain-text) notification email to the site owner
 * whenever a visitor submits the contact form.  The visitor's email address is
 * set as the {@code reply_to} field so replies go directly to them.</p>
 *
 * <p>Transport is handled via Java 21's built-in {@link HttpClient} — no
 * additional dependencies are required.  All credentials are read from
 * environment variables and never logged.</p>
 *
 * <p>Required environment variables:</p>
 * <ul>
 *   <li>{@code RESEND_API_KEY}    — Resend API secret key</li>
 *   <li>{@code RESEND_FROM_EMAIL} — Verified sender address (e.g. {@code onboarding@resend.dev})</li>
 *   <li>{@code CONTACT_RECIPIENT_EMAIL} — Inbox that receives contact notifications</li>
 * </ul>
 */
@Service
public class ResendEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailService.class);

    private static final String RESEND_API_URL = "https://api.resend.com/emails";
    private static final int    HTTP_OK_MIN    = 200;
    private static final int    HTTP_OK_MAX    = 299;

    private final AppProperties        appProperties;
    private final EmailTemplateService templateService;
    private final HttpClient           httpClient;
    private final ObjectMapper         objectMapper;

    /**
     * Production constructor — used by Spring to wire the bean.
     * Builds its own {@link HttpClient} with a 10-second connect timeout.
     */
    @Autowired
    public ResendEmailService(AppProperties appProperties,
                              EmailTemplateService templateService) {
        this(appProperties, templateService,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build(),
                new ObjectMapper());
    }

    /**
     * Package-private constructor used by tests to inject a mock {@link HttpClient}
     * and {@link ObjectMapper}, keeping the public constructor clean.
     */
    ResendEmailService(AppProperties appProperties,
                       EmailTemplateService templateService,
                       HttpClient httpClient,
                       ObjectMapper objectMapper) {
        this.appProperties   = appProperties;
        this.templateService = templateService;
        this.httpClient      = httpClient;
        this.objectMapper    = objectMapper;
    }

    @Override
    public void sendContactEmail(ContactRequest request) {
        String recipient  = appProperties.contact().recipientEmail();
        String apiKey     = appProperties.resend().apiKey();
        String fromEmail  = appProperties.resend().fromEmail();

        // ── Guard: recipient must be configured ──────────────────────────────
        if (recipient == null || recipient.isBlank()) {
            log.error("CONTACT_RECIPIENT_EMAIL is not configured — cannot send email");
            throw new ContactServiceException(
                    "Email delivery is not configured on the server.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        // ── Guard: API key must be present ───────────────────────────────────
        if (apiKey == null || apiKey.isBlank()) {
            log.error("RESEND_API_KEY is not configured — cannot send email");
            throw new ContactServiceException(
                    "Email delivery is not configured on the server.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        log.info("Sending contact notification via Resend to '{}' — from: '{}' <{}>",
                recipient, request.name(), request.email());

        try {
            EmailTemplateService.EmailContent content = templateService.buildContactEmail(request);

            // Build the JSON payload for the Resend /emails endpoint.
            // reply_to routes direct replies to the visitor.
            String payload = objectMapper.writeValueAsString(Map.of(
                    "from",     fromEmail,
                    "to",       new String[]{ recipient },
                    "reply_to", request.email(),
                    "subject",  "Portfolio Contact: " + request.subject(),
                    "html",     content.html(),
                    "text",     content.text()
            ));

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(RESEND_API_URL))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();

            if (statusCode < HTTP_OK_MIN || statusCode > HTTP_OK_MAX) {
                // Log status code only — never log the response body (may contain key echoes).
                log.error("Resend API returned non-2xx status {} for contact from '{}'",
                        statusCode, request.email());
                throw new ContactServiceException(
                        "Failed to send your message. Please try again later.",
                        HttpStatus.INTERNAL_SERVER_ERROR
                );
            }

            log.info("Contact notification sent via Resend (status {}) to '{}'",
                    statusCode, recipient);

        } catch (ContactServiceException e) {
            // Re-throw our own typed exceptions without wrapping.
            throw e;
        } catch (Exception e) {
            // InterruptedException, IOException, JSON serialisation errors — all
            // map to the same safe user-facing message.  Never log the exception
            // message if it could contain the API key or credentials.
            log.error("Resend API call failed for contact from '{}': {}",
                    request.email(), sanitise(e.getMessage()));
            // Restore interrupt flag if this was an InterruptedException.
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ContactServiceException(
                    "Failed to send your message. Please try again later.",
                    HttpStatus.INTERNAL_SERVER_ERROR, e
            );
        }
    }

    /**
     * Strips anything that looks like a bearer token or API key from an error
     * message before it is written to the log.
     */
    private static String sanitise(String message) {
        if (message == null) return "(null)";
        // Redact anything that looks like re_<token> (Resend key format).
        return message.replaceAll("re_[A-Za-z0-9_]+", "[REDACTED]")
                      .replaceAll("Bearer [A-Za-z0-9_\\-\\.]+", "Bearer [REDACTED]");
    }
}
