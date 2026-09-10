package dev.sanket.contactservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test — verifies the Spring application context loads cleanly.
 *
 * SMTP credentials are stubbed so the context starts without a live Gmail
 * connection. No real email is sent during this test.
 */
@SpringBootTest
@ActiveProfiles("local")
@TestPropertySource(properties = {
        "spring.mail.host=localhost",
        "spring.mail.port=3025",
        "spring.mail.username=test@example.com",
        "spring.mail.password=test",
        "spring.mail.properties.mail.smtp.auth=false",
        "spring.mail.properties.mail.smtp.starttls.enable=false",
        "spring.mail.properties.mail.smtp.starttls.required=false",
        "app.contact.recipient-email=test@example.com"
})
class ContactServiceApplicationTests {

    @Test
    void contextLoads() {
        // If the context fails to start, this test fails — no assertions needed.
    }
}
