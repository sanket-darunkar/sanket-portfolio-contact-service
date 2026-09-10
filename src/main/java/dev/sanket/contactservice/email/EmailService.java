package dev.sanket.contactservice.email;

import dev.sanket.contactservice.dto.ContactRequest;

/**
 * Contract for sending contact form notification emails.
 * Kept separate from {@link dev.sanket.contactservice.service.ContactService}
 * so the transport mechanism (SMTP, SendGrid, SES, etc.) can be swapped
 * without touching business logic.
 */
public interface EmailService {

    /**
     * Send a contact form notification email to the site owner.
     *
     * @param request the validated contact form data
     * @throws dev.sanket.contactservice.exception.ContactServiceException
     *         if the email could not be delivered
     */
    void sendContactEmail(ContactRequest request);
}
