package dev.sanket.contactservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sanket.contactservice.dto.ContactRequest;
import dev.sanket.contactservice.dto.ContactResponse;
import dev.sanket.contactservice.exception.ContactServiceException;
import dev.sanket.contactservice.service.ContactService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice test for {@link ContactController}.
 * Uses {@link WebMvcTest} — only the web layer is loaded.
 * ContactService is mocked: no real email is sent.
 */
@WebMvcTest(ContactController.class)
class ContactControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ContactService contactService;

    private static final String URL = "/api/contact";

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/contact with valid payload returns 200 and success body")
    void submitContact_validRequest_returns200() throws Exception {
        when(contactService.processContact(any())).thenReturn(ContactResponse.ok());

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    // ── Email failure ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/contact returns 500 when email delivery fails")
    void submitContact_emailDeliveryFails_returns500() throws Exception {
        when(contactService.processContact(any()))
                .thenThrow(new ContactServiceException(
                        "Failed to send your message. Please try again later.",
                        HttpStatus.INTERNAL_SERVER_ERROR
                ));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Failed to send your message. Please try again later."));
    }

    @Test
    @DisplayName("POST /api/contact 500 response does not expose SMTP credentials")
    void submitContact_emailDeliveryFails_responseDoesNotExposeCredentials() throws Exception {
        when(contactService.processContact(any()))
                .thenThrow(new ContactServiceException(
                        "Failed to send your message. Please try again later.",
                        HttpStatus.INTERNAL_SERVER_ERROR
                ));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Failed to send your message. Please try again later."));
    }

    // ── Validation failures ───────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/contact with blank name returns 400 with field error")
    void submitContact_blankName_returns400() throws Exception {
        ContactRequest request = new ContactRequest(
                "",
                "jane@example.com",
                "Subject",
                "A message long enough to pass the minimum length check."
        );

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    @DisplayName("POST /api/contact with invalid email returns 400 with field error")
    void submitContact_invalidEmail_returns400() throws Exception {
        ContactRequest request = new ContactRequest(
                "Jane Doe",
                "not-an-email",
                "Subject",
                "A message long enough to pass the minimum length check."
        );

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    @DisplayName("POST /api/contact with blank subject returns 400 with field error")
    void submitContact_blankSubject_returns400() throws Exception {
        ContactRequest request = new ContactRequest(
                "Jane Doe",
                "jane@example.com",
                "",
                "A message long enough to pass the minimum length check."
        );

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.subject").exists());
    }

    @Test
    @DisplayName("POST /api/contact with blank message returns 400 with field error")
    void submitContact_blankMessage_returns400() throws Exception {
        ContactRequest request = new ContactRequest(
                "Jane Doe",
                "jane@example.com",
                "Subject",
                ""
        );

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.message").exists());
    }

    @Test
    @DisplayName("POST /api/contact with missing body returns 400")
    void submitContact_missingBody_returns400() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ContactRequest validRequest() {
        return new ContactRequest(
                "Jane Doe",
                "jane@example.com",
                "Hello there",
                "This is a test message with enough content."
        );
    }
}
