package dev.sanket.contactservice.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sanket.contactservice.config.AppProperties;
import dev.sanket.contactservice.dto.ContactRequest;
import dev.sanket.contactservice.exception.ContactServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ResendEmailService}.
 *
 * <p>{@link HttpClient} is mocked so no real HTTP requests are made and no
 * real emails are sent.  The package-private constructor is used to inject
 * both the mock client and a real {@link ObjectMapper}.</p>
 */
@ExtendWith(MockitoExtension.class)
class ResendEmailServiceTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private EmailTemplateService    templateService;
    private ResendEmailService      emailService;

    private static final String VALID_API_KEY    = "re_test_key_123";
    private static final String VALID_FROM       = "onboarding@resend.dev";
    private static final String VALID_RECIPIENT  = "recipient@example.com";

    @BeforeEach
    void setUp() {
        templateService = new EmailTemplateService();
        emailService = new ResendEmailService(
                buildAppProperties(VALID_RECIPIENT, VALID_API_KEY, VALID_FROM),
                templateService,
                httpClient,
                new ObjectMapper()
        );
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Successful email delivery")
    class SuccessPath {

        @Test
        @DisplayName("sendContactEmail posts to the Resend API endpoint without throwing")
        @SuppressWarnings("unchecked")
        void sendContactEmail_validRequest_postsToResendApi() throws Exception {
            when(httpResponse.statusCode()).thenReturn(200);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);

            emailService.sendContactEmail(validRequest());

            verify(httpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        }

        @Test
        @DisplayName("sendContactEmail sends POST to https://api.resend.com/emails")
        @SuppressWarnings("unchecked")
        void sendContactEmail_usesCorrectEndpoint() throws Exception {
            when(httpResponse.statusCode()).thenReturn(200);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);

            emailService.sendContactEmail(validRequest());

            ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
            verify(httpClient).send(captor.capture(), any(HttpResponse.BodyHandler.class));
            assertThat(captor.getValue().uri().toString())
                    .isEqualTo("https://api.resend.com/emails");
        }

        @Test
        @DisplayName("sendContactEmail sends HTTP POST method")
        @SuppressWarnings("unchecked")
        void sendContactEmail_usesPostMethod() throws Exception {
            when(httpResponse.statusCode()).thenReturn(200);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);

            emailService.sendContactEmail(validRequest());

            ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
            verify(httpClient).send(captor.capture(), any(HttpResponse.BodyHandler.class));
            assertThat(captor.getValue().method()).isEqualTo("POST");
        }

        @Test
        @DisplayName("sendContactEmail includes Authorization Bearer header")
        @SuppressWarnings("unchecked")
        void sendContactEmail_includesBearerAuthHeader() throws Exception {
            when(httpResponse.statusCode()).thenReturn(200);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);

            emailService.sendContactEmail(validRequest());

            ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
            verify(httpClient).send(captor.capture(), any(HttpResponse.BodyHandler.class));
            assertThat(captor.getValue().headers().firstValue("Authorization"))
                    .hasValue("Bearer " + VALID_API_KEY);
        }

        @Test
        @DisplayName("sendContactEmail includes Content-Type: application/json header")
        @SuppressWarnings("unchecked")
        void sendContactEmail_includesContentTypeHeader() throws Exception {
            when(httpResponse.statusCode()).thenReturn(200);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);

            emailService.sendContactEmail(validRequest());

            ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
            verify(httpClient).send(captor.capture(), any(HttpResponse.BodyHandler.class));
            assertThat(captor.getValue().headers().firstValue("Content-Type"))
                    .hasValue("application/json");
        }

        @Test
        @DisplayName("sendContactEmail accepts 201 as a success status")
        @SuppressWarnings("unchecked")
        void sendContactEmail_201StatusCode_doesNotThrow() throws Exception {
            when(httpResponse.statusCode()).thenReturn(201);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);

            // Should not throw — 201 is within the 2xx range
            emailService.sendContactEmail(validRequest());
        }
    }

    // ── Request payload assertions ────────────────────────────────────────────

    @Nested
    @DisplayName("Request payload")
    class Payload {

        @Test
        @DisplayName("payload contains the recipient email in the 'to' field")
        @SuppressWarnings("unchecked")
        void payload_containsRecipient() throws Exception {
            when(httpResponse.statusCode()).thenReturn(200);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);

            emailService.sendContactEmail(validRequest());

            ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
            verify(httpClient).send(captor.capture(), any(HttpResponse.BodyHandler.class));
            String body = captor.getValue().bodyPublisher()
                    .map(p -> {
                        var subscriber = new java.util.concurrent.Flow.Subscriber<java.nio.ByteBuffer>() {
                            final java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                            @Override public void onSubscribe(java.util.concurrent.Flow.Subscription s) { s.request(Long.MAX_VALUE); }
                            @Override public void onNext(java.nio.ByteBuffer b) { byte[] arr = new byte[b.remaining()]; b.get(arr); baos.write(arr, 0, arr.length); }
                            @Override public void onError(Throwable t) {}
                            @Override public void onComplete() {}
                            String result() { return baos.toString(java.nio.charset.StandardCharsets.UTF_8); }
                        };
                        p.subscribe(subscriber);
                        return subscriber.result();
                    }).orElse("");
            assertThat(body).contains(VALID_RECIPIENT);
        }

        @Test
        @DisplayName("payload contains visitor email as reply_to")
        @SuppressWarnings("unchecked")
        void payload_containsReplyTo() throws Exception {
            when(httpResponse.statusCode()).thenReturn(200);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);

            emailService.sendContactEmail(validRequest());

            ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
            verify(httpClient).send(captor.capture(), any(HttpResponse.BodyHandler.class));
            // Extract body string via publisher
            String body = readBody(captor.getValue());
            assertThat(body).contains("reply_to").contains("jane@example.com");
        }
    }

    // ── HTML template assertions (transport-independent) ─────────────────────

    @Nested
    @DisplayName("Email HTML template content")
    class TemplateContent {

        @Test
        @DisplayName("buildContactEmail HTML contains visitor name")
        void template_htmlContainsName() {
            EmailTemplateService.EmailContent content =
                    templateService.buildContactEmail(validRequest());
            assertThat(content.html()).contains("Jane Doe");
        }

        @Test
        @DisplayName("buildContactEmail HTML contains visitor email")
        void template_htmlContainsEmail() {
            EmailTemplateService.EmailContent content =
                    templateService.buildContactEmail(validRequest());
            assertThat(content.html()).contains("jane@example.com");
        }

        @Test
        @DisplayName("buildContactEmail HTML contains subject")
        void template_htmlContainsSubject() {
            EmailTemplateService.EmailContent content =
                    templateService.buildContactEmail(validRequest());
            assertThat(content.html()).contains("Hello there");
        }

        @Test
        @DisplayName("buildContactEmail HTML contains message body")
        void template_htmlContainsMessage() {
            EmailTemplateService.EmailContent content =
                    templateService.buildContactEmail(validRequest());
            assertThat(content.html()).contains("This is a test message");
        }

        @Test
        @DisplayName("buildContactEmail HTML contains Reply-to CTA with visitor email")
        void template_htmlContainsReplyToCta() {
            EmailTemplateService.EmailContent content =
                    templateService.buildContactEmail(validRequest());
            assertThat(content.html())
                    .contains("mailto:jane@example.com")
                    .contains("Reply to Jane Doe");
        }

        @Test
        @DisplayName("buildContactEmail HTML contains portfolio footer text")
        void template_htmlContainsFooter() {
            EmailTemplateService.EmailContent content =
                    templateService.buildContactEmail(validRequest());
            assertThat(content.html())
                    .contains("Sanket")
                    .contains("Portfolio");
        }

        @Test
        @DisplayName("buildContactEmail HTML is valid markup with doctype")
        void template_htmlHasDoctype() {
            EmailTemplateService.EmailContent content =
                    templateService.buildContactEmail(validRequest());
            assertThat(content.html())
                    .startsWith("<!DOCTYPE html>")
                    .contains("<html")
                    .contains("</html>");
        }

        @Test
        @DisplayName("buildContactEmail plain-text fallback contains all key fields")
        void template_plainTextContainsAllFields() {
            EmailTemplateService.EmailContent content =
                    templateService.buildContactEmail(validRequest());
            assertThat(content.text())
                    .contains("Jane Doe")
                    .contains("jane@example.com")
                    .contains("Hello there")
                    .contains("This is a test message")
                    .contains("NEW PORTFOLIO CONTACT")
                    .contains("Sanket");
        }
    }

    // ── HTML escaping ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("HTML escaping in template")
    class HtmlEscaping {

        @Test
        @DisplayName("HTML template escapes angle brackets in visitor name")
        void template_escapesHtmlInName() {
            ContactRequest xssRequest = new ContactRequest(
                    "<script>alert('xss')</script>",
                    "jane@example.com",
                    "Subject",
                    "A message with enough content to pass validation."
            );
            EmailTemplateService.EmailContent content =
                    templateService.buildContactEmail(xssRequest);
            assertThat(content.html())
                    .doesNotContain("<script>")
                    .contains("&lt;script&gt;");
        }

        @Test
        @DisplayName("HTML template escapes ampersands in subject")
        void template_escapesAmpersandInSubject() {
            ContactRequest request = new ContactRequest(
                    "Jane Doe",
                    "jane@example.com",
                    "Rock & Roll",
                    "A message with enough content to pass validation."
            );
            EmailTemplateService.EmailContent content =
                    templateService.buildContactEmail(request);
            assertThat(content.html())
                    .doesNotContain("Rock & Roll")
                    .contains("Rock &amp; Roll");
        }

        @Test
        @DisplayName("EmailTemplateService.escape handles null gracefully")
        void escape_nullInput_returnsEmpty() {
            assertThat(EmailTemplateService.escape(null)).isEmpty();
        }
    }

    // ── Failure paths ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Failure and guard conditions")
    class FailurePaths {

        @Test
        @DisplayName("sendContactEmail throws ContactServiceException when Resend returns 4xx")
        @SuppressWarnings("unchecked")
        void sendContactEmail_resend4xx_throwsContactServiceException() throws Exception {
            when(httpResponse.statusCode()).thenReturn(401);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);

            assertThatThrownBy(() -> emailService.sendContactEmail(validRequest()))
                    .isInstanceOf(ContactServiceException.class)
                    .extracting(ex -> ((ContactServiceException) ex).getStatus())
                    .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("sendContactEmail throws ContactServiceException when Resend returns 5xx")
        @SuppressWarnings("unchecked")
        void sendContactEmail_resend5xx_throwsContactServiceException() throws Exception {
            when(httpResponse.statusCode()).thenReturn(503);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);

            assertThatThrownBy(() -> emailService.sendContactEmail(validRequest()))
                    .isInstanceOf(ContactServiceException.class)
                    .extracting(ex -> ((ContactServiceException) ex).getStatus())
                    .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("sendContactEmail throws ContactServiceException when HttpClient throws IOException")
        @SuppressWarnings("unchecked")
        void sendContactEmail_ioException_throwsContactServiceException() throws Exception {
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenThrow(new IOException("Connection refused"));

            assertThatThrownBy(() -> emailService.sendContactEmail(validRequest()))
                    .isInstanceOf(ContactServiceException.class)
                    .extracting(ex -> ((ContactServiceException) ex).getStatus())
                    .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("sendContactEmail exception message does not expose API key")
        @SuppressWarnings("unchecked")
        void sendContactEmail_failure_messageDoesNotExposeApiKey() throws Exception {
            when(httpResponse.statusCode()).thenReturn(500);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);

            assertThatThrownBy(() -> emailService.sendContactEmail(validRequest()))
                    .isInstanceOf(ContactServiceException.class)
                    .hasMessageNotContaining(VALID_API_KEY)
                    .hasMessageNotContaining("re_test_key");
        }

        @Test
        @DisplayName("sendContactEmail throws ContactServiceException when recipient email is blank")
        void sendContactEmail_blankRecipient_throwsContactServiceException() {
            ResendEmailService serviceWithNoRecipient = new ResendEmailService(
                    buildAppProperties("", VALID_API_KEY, VALID_FROM),
                    templateService,
                    httpClient,
                    new ObjectMapper()
            );

            assertThatThrownBy(() -> serviceWithNoRecipient.sendContactEmail(validRequest()))
                    .isInstanceOf(ContactServiceException.class)
                    .extracting(ex -> ((ContactServiceException) ex).getStatus())
                    .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

            verifyNoInteractions(httpClient);
        }

        @Test
        @DisplayName("sendContactEmail throws ContactServiceException when API key is blank")
        void sendContactEmail_blankApiKey_throwsContactServiceException() {
            ResendEmailService serviceWithNoKey = new ResendEmailService(
                    buildAppProperties(VALID_RECIPIENT, "", VALID_FROM),
                    templateService,
                    httpClient,
                    new ObjectMapper()
            );

            assertThatThrownBy(() -> serviceWithNoKey.sendContactEmail(validRequest()))
                    .isInstanceOf(ContactServiceException.class)
                    .extracting(ex -> ((ContactServiceException) ex).getStatus())
                    .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

            verifyNoInteractions(httpClient);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ContactRequest validRequest() {
        return new ContactRequest(
                "Jane Doe",
                "jane@example.com",
                "Hello there",
                "This is a test message with enough content to pass validation."
        );
    }

    private AppProperties buildAppProperties(String recipientEmail,
                                              String apiKey,
                                              String fromEmail) {
        return new AppProperties(
                new AppProperties.Cors(List.of("http://localhost:3000")),
                new AppProperties.Resend(apiKey, fromEmail),
                new AppProperties.Contact(recipientEmail)
        );
    }

    /**
     * Reads the body string from an {@link HttpRequest}'s body publisher.
     */
    private String readBody(HttpRequest request) {
        return request.bodyPublisher().map(p -> {
            var sub = new java.util.concurrent.Flow.Subscriber<java.nio.ByteBuffer>() {
                final java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                @Override public void onSubscribe(java.util.concurrent.Flow.Subscription s) { s.request(Long.MAX_VALUE); }
                @Override public void onNext(java.nio.ByteBuffer b) { byte[] a = new byte[b.remaining()]; b.get(a); baos.write(a, 0, a.length); }
                @Override public void onError(Throwable t) {}
                @Override public void onComplete() {}
                String result() { return baos.toString(java.nio.charset.StandardCharsets.UTF_8); }
            };
            p.subscribe(sub);
            return sub.result();
        }).orElse("");
    }
}
