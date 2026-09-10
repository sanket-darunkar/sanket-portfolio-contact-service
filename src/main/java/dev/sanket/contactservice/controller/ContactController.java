package dev.sanket.contactservice.controller;

import dev.sanket.contactservice.dto.ContactRequest;
import dev.sanket.contactservice.dto.ContactResponse;
import dev.sanket.contactservice.service.ContactService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the contact form endpoint.
 *
 * Responsibilities: HTTP mapping, input validation trigger, delegation to service.
 * All business logic lives in {@link ContactService}.
 */
@RestController
@RequestMapping("/api/contact")
public class ContactController {

    private static final Logger log = LoggerFactory.getLogger(ContactController.class);

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    /**
     * POST /api/contact
     *
     * Accepts a contact form submission, validates it, and delegates
     * processing to the service layer.
     *
     * @param request validated contact form payload
     * @return 200 OK with a {@link ContactResponse} on success
     */
    @PostMapping
    public ResponseEntity<ContactResponse> submitContact(@Valid @RequestBody ContactRequest request) {
        log.debug("POST /api/contact — processing request from '{}'", request.email());
        ContactResponse response = contactService.processContact(request);
        return ResponseEntity.ok(response);
    }
}
