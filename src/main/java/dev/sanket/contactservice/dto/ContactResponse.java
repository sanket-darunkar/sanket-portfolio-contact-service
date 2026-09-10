package dev.sanket.contactservice.dto;

/**
 * Outbound DTO returned after a contact request is processed.
 */
public record ContactResponse(
        boolean success,
        String message
) {

    public static ContactResponse ok() {
        return new ContactResponse(true, "Your message has been received. I'll get back to you soon.");
    }
}
