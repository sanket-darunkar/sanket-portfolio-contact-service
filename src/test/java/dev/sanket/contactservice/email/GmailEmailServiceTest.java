package dev.sanket.contactservice.email;

import dev.sanket.contactservice.config.AppProperties;
import dev.sanket.contactservice.dto.ContactRequest;
import dev.sanket.contactservice.exception.ContactServiceException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GmailEmailService}.
 * JavaMailSender is mocked — no real emails are sent.
 *
 * For happy-path tests, createMimeMessage() returns a real MimeMessage
 * (from JavaMailSenderImpl) so MimeMessageHelper can set headers/parts on
 * it without NullPointerExceptions. The actual send() call is intercepted
 * by the mock — nothing hits the network.
 */
@ExtendWith(MockitoExtension.class)
class GmailEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private AppProperties appProperties;
    private EmailTemplateService templateService;
    private GmailEmailService emailService;

    /** Provides real MimeMessage instances for helper population. */
    private final JavaMailSenderImpl realSender = new JavaMailSenderImpl();

    @BeforeEach
    void setUp() {
        appProperties   = buildAppProperties("recipient@example.com");
        templateService = new EmailTemplateService();
        emailService    = new GmailEmailService(mailSender, appProperties, templateService);
    }

    // ── Happy path — send ─────────────────────────────────────────────────────

    @Test
    @DisplayName("sendContactEmail creates and sends a MimeMessage without throwing")
    void sendContactEmail_validRequest_sendsMessage() {
        when(mailSender.createMimeMessage()).thenReturn(realSender.createMimeMessage());

        emailService.sendContactEmail(validRequest());

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendContactEmail calls mailSender.send exactly once")
    void sendContactEmail_callsSendOnce() {
        when(mailSender.createMimeMessage()).thenReturn(realSender.createMimeMessage());

        emailService.sendContactEmail(validRequest());

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    // ── HTML content assertions ───────────────────────────────────────────────

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

    // ── HTML escaping ─────────────────────────────────────────────────────────

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

    // ── Reply-To header ───────────────────────────────────────────────────────

    @Test
    @DisplayName("sendContactEmail sets Reply-To to visitor email address")
    void sendContactEmail_setsReplyToVisitorEmail() throws Exception {
        MimeMessage realMessage = realSender.createMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(realMessage);

        emailService.sendContactEmail(validRequest());

        // Reply-To header should contain the visitor's address
        String[] replyTo = realMessage.getHeader("Reply-To");
        assertThat(replyTo).isNotNull().isNotEmpty();
        assertThat(replyTo[0]).contains("jane@example.com");
    }

    // ── Failure path ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("sendContactEmail throws ContactServiceException when SMTP fails")
    void sendContactEmail_smtpFailure_throwsContactServiceException() {
        when(mailSender.createMimeMessage()).thenReturn(realSender.createMimeMessage());
        doThrow(new MailSendException("Connection refused"))
                .when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> emailService.sendContactEmail(validRequest()))
                .isInstanceOf(ContactServiceException.class)
                .extracting(ex -> ((ContactServiceException) ex).getStatus())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("sendContactEmail throws ContactServiceException when recipient email is blank")
    void sendContactEmail_blankRecipient_throwsContactServiceException() {
        AppProperties noRecipient = buildAppProperties("");
        GmailEmailService serviceWithNoRecipient =
                new GmailEmailService(mailSender, noRecipient, templateService);

        assertThatThrownBy(() -> serviceWithNoRecipient.sendContactEmail(validRequest()))
                .isInstanceOf(ContactServiceException.class)
                .extracting(ex -> ((ContactServiceException) ex).getStatus())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        verifyNoInteractions(mailSender);
    }

    @Test
    @DisplayName("sendContactEmail exception message does not expose SMTP credentials")
    void sendContactEmail_smtpFailure_messageDoesNotExposeCredentials() {
        when(mailSender.createMimeMessage()).thenReturn(realSender.createMimeMessage());
        doThrow(new MailSendException("auth failed: password=secret123"))
                .when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> emailService.sendContactEmail(validRequest()))
                .isInstanceOf(ContactServiceException.class)
                .hasMessageNotContaining("secret123")
                .hasMessageNotContaining("password");
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

    private AppProperties buildAppProperties(String recipientEmail) {
        return new AppProperties(
                new AppProperties.Cors(List.of("http://localhost:3000")),
                new AppProperties.Contact(recipientEmail)
        );
    }
}
