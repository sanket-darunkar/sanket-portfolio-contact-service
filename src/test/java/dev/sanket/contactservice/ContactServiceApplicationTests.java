package dev.sanket.contactservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test — verifies the Spring application context loads cleanly.
 *
 * Resend credentials are stubbed with placeholder values so the context
 * starts without requiring real environment variables.  No real email
 * is sent during this test.
 */
@SpringBootTest
@ActiveProfiles("local")
@TestPropertySource(properties = {
        "app.resend.api-key=re_test_placeholder",
        "app.resend.from-email=onboarding@resend.dev",
        "app.contact.recipient-email=test@example.com"
})
class ContactServiceApplicationTests {

    @Test
    void contextLoads() {
        // If the context fails to start, this test fails — no assertions needed.
    }
}
