package dev.sanket.contactservice.email;

import dev.sanket.contactservice.config.AppProperties;
import dev.sanket.contactservice.dto.ContactRequest;
import dev.sanket.contactservice.exception.ContactServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Gmail SMTP implementation of {@link EmailService}.
 *
 * Sends a multipart (HTML + plain-text fallback) notification email to the
 * site owner whenever a visitor submits the contact form. The visitor's email
 * is set as the Reply-To header so replies go directly to them.
 *
 * All credentials and addresses are read from environment variables —
 * nothing is hardcoded. HTML content is produced by {@link EmailTemplateService},
 * which HTML-escapes all visitor-supplied values before insertion.
 */
@Service
public class GmailEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(GmailEmailService.class);

    private final JavaMailSender mailSender;
    private final AppProperties appProperties;
    private final EmailTemplateService templateService;

    public GmailEmailService(JavaMailSender mailSender,
                             AppProperties appProperties,
                             EmailTemplateService templateService) {
        this.mailSender      = mailSender;
        this.appProperties   = appProperties;
        this.templateService = templateService;
    }

    @Override
    public void sendContactEmail(ContactRequest request) {
        String recipient = appProperties.contact().recipientEmail();

        if (recipient == null || recipient.isBlank()) {
            log.error("CONTACT_RECIPIENT_EMAIL is not configured — cannot send email");
            throw new ContactServiceException(
                    "Email delivery is not configured on the server.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        log.info("Sending contact notification email to '{}' — from: '{}' <{}>",
                recipient, request.name(), request.email());

        try {
            EmailTemplateService.EmailContent content = templateService.buildContactEmail(request);

            MimeMessage message = mailSender.createMimeMessage();
            // multipart=true enables both HTML and plain-text parts
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(recipient);
            helper.setReplyTo(request.email());
            helper.setSubject("Portfolio Contact: " + request.subject());
            // setText(html, plainText) — first arg is the HTML body when multipart is true;
            // second arg is the plain-text alternative shown by clients that don't render HTML
            helper.setText(content.text(), content.html());

            mailSender.send(message);

            log.info("Contact notification email sent successfully to '{}'", recipient);

        } catch (MessagingException e) {
            log.error("Failed to build contact notification email: {}", e.getMessage());
            throw new ContactServiceException(
                    "Failed to send your message. Please try again later.",
                    HttpStatus.INTERNAL_SERVER_ERROR, e
            );
        } catch (MailException e) {
            log.error("SMTP delivery failed for contact from '{}': {}", request.email(), e.getMessage());
            throw new ContactServiceException(
                    "Failed to send your message. Please try again later.",
                    HttpStatus.INTERNAL_SERVER_ERROR, e
            );
        }
    }
}
