package dev.sanket.contactservice.service;

import dev.sanket.contactservice.dto.ContactRequest;
import dev.sanket.contactservice.dto.ContactResponse;
import dev.sanket.contactservice.email.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Default implementation of {@link ContactService}.
 *
 * Delegates email delivery to {@link EmailService}. If delivery fails,
 * the exception propagates to the global handler — success is only reported
 * after the email has actually been submitted to the SMTP relay.
 */
@Service
public class ContactServiceImpl implements ContactService {

    private static final Logger log = LoggerFactory.getLogger(ContactServiceImpl.class);

    private final EmailService emailService;

    public ContactServiceImpl(EmailService emailService) {
        this.emailService = emailService;
    }

    @Override
    public ContactResponse processContact(ContactRequest request) {
        log.info("Contact form submission received — from: '{}' <{}>, subject: '{}'",
                request.name(), request.email(), request.subject());

        log.debug("Message preview (first 100 chars): {}",
                request.message().length() > 100
                        ? request.message().substring(0, 100) + "..."
                        : request.message());

        // Throws ContactServiceException (→ 500) if delivery fails.
        // The exception propagates to GlobalExceptionHandler — no false success.
        emailService.sendContactEmail(request);

        return ContactResponse.ok();
    }
}
