package dev.sanket.contactservice.service;

import dev.sanket.contactservice.dto.ContactRequest;
import dev.sanket.contactservice.dto.ContactResponse;

/**
 * Contract for handling incoming contact form submissions.
 * Keeping this as an interface makes it easy to swap implementations
 * (e.g. swap mock → email-sending) without touching the controller.
 */
public interface ContactService {

    /**
     * Process a validated contact request.
     *
     * @param request the inbound contact form data
     * @return a response indicating success or failure
     */
    ContactResponse processContact(ContactRequest request);
}
