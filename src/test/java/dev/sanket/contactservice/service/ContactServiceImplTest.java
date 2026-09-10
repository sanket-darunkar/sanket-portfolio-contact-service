package dev.sanket.contactservice.service;

import dev.sanket.contactservice.dto.ContactRequest;
import dev.sanket.contactservice.dto.ContactResponse;
import dev.sanket.contactservice.email.EmailService;
import dev.sanket.contactservice.exception.ContactServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ContactServiceImpl}.
 * No Spring context — pure JUnit 5 + Mockito + AssertJ.
 * EmailService is mocked: no real emails are sent.
 */
@ExtendWith(MockitoExtension.class)
class ContactServiceImplTest {

    @Mock
    private EmailService emailService;

    private ContactServiceImpl contactService;

    @BeforeEach
    void setUp() {
        contactService = new ContactServiceImpl(emailService);
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("processContact returns success when email is sent successfully")
    void processContact_validRequest_emailSent_returnsSuccess() {
        ContactRequest request = validRequest();
        doNothing().when(emailService).sendContactEmail(any());

        ContactResponse response = contactService.processContact(request);

        assertThat(response).isNotNull();
        assertThat(response.success()).isTrue();
        assertThat(response.message()).isNotBlank();
        verify(emailService, times(1)).sendContactEmail(request);
    }

    @Test
    @DisplayName("processContact delegates to EmailService with the exact request")
    void processContact_delegatesToEmailService() {
        ContactRequest request = validRequest();
        doNothing().when(emailService).sendContactEmail(request);

        contactService.processContact(request);

        verify(emailService).sendContactEmail(request);
        verifyNoMoreInteractions(emailService);
    }

    @Test
    @DisplayName("processContact handles a long message without throwing")
    void processContact_longMessage_doesNotThrow() {
        String longMessage = "A".repeat(5000);
        ContactRequest request = new ContactRequest(
                "John Smith",
                "john@example.com",
                "Long message subject",
                longMessage
        );
        doNothing().when(emailService).sendContactEmail(any());

        ContactResponse response = contactService.processContact(request);

        assertThat(response.success()).isTrue();
    }

    // ── Failure path ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("processContact propagates ContactServiceException when email delivery fails")
    void processContact_emailDeliveryFails_throwsContactServiceException() {
        ContactRequest request = validRequest();
        ContactServiceException emailFailure = new ContactServiceException(
                "Failed to send your message. Please try again later.",
                HttpStatus.INTERNAL_SERVER_ERROR
        );
        doThrow(emailFailure).when(emailService).sendContactEmail(any());

        assertThatThrownBy(() -> contactService.processContact(request))
                .isInstanceOf(ContactServiceException.class)
                .hasMessageContaining("Failed to send")
                .extracting(ex -> ((ContactServiceException) ex).getStatus())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("processContact does NOT return success when email delivery fails")
    void processContact_emailDeliveryFails_doesNotReturnSuccess() {
        ContactRequest request = validRequest();
        doThrow(new ContactServiceException("SMTP error", HttpStatus.INTERNAL_SERVER_ERROR))
                .when(emailService).sendContactEmail(any());

        assertThatThrownBy(() -> contactService.processContact(request))
                .isInstanceOf(ContactServiceException.class);

        // Confirm we never reach ContactResponse.ok()
        verify(emailService).sendContactEmail(request);
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
}
