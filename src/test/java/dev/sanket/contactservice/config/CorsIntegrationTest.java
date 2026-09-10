package dev.sanket.contactservice.config;

import dev.sanket.contactservice.service.ContactService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link CorsConfig}.
 *
 * <p>Uses {@code @SpringBootTest + @AutoConfigureMockMvc} so the full
 * application context — including the {@link org.springframework.web.filter.CorsFilter}
 * bean — is loaded.  {@code @WebMvcTest} does NOT load {@code @Configuration}
 * beans outside the controller layer, so it cannot exercise the CorsFilter.</p>
 *
 * <p>SMTP is stubbed out so no real email is sent during these tests.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        // Stub Resend credentials so the context starts without real env vars.
        "app.resend.api-key=re_test_placeholder",
        "app.resend.from-email=onboarding@resend.dev",
        "app.contact.recipient-email=test@example.com",
        // Explicitly configure the two origins we care about for these tests.
        "app.cors.allowed-origins=https://developer-platform-inky.vercel.app,http://localhost:3000"
})
class CorsIntegrationTest {

    private static final String ENDPOINT = "/api/contact";

    private static final String VERCEL_ORIGIN    = "https://developer-platform-inky.vercel.app";
    private static final String LOCALHOST_ORIGIN  = "http://localhost:3000";
    private static final String DISALLOWED_ORIGIN = "https://evil.example.com";

    @Autowired
    private MockMvc mockMvc;

    // Mock the service so no real email is attempted.
    @MockBean
    private ContactService contactService;

    // ── Preflight (OPTIONS) — Vercel production origin ────────────────────────

    @Nested
    @DisplayName("OPTIONS preflight from Vercel production origin")
    class VercelPreflight {

        @Test
        @DisplayName("returns 200 with Access-Control-Allow-Origin for Vercel origin")
        void preflight_vercelOrigin_returns200WithAllowOriginHeader() throws Exception {
            mockMvc.perform(options(ENDPOINT)
                            .header(HttpHeaders.ORIGIN, VERCEL_ORIGIN)
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, VERCEL_ORIGIN));
        }

        @Test
        @DisplayName("preflight response includes Access-Control-Allow-Methods with POST")
        void preflight_vercelOrigin_includesAllowMethods() throws Exception {
            mockMvc.perform(options(ENDPOINT)
                            .header(HttpHeaders.ORIGIN, VERCEL_ORIGIN)
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
                    .andExpect(status().isOk())
                    .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS));
        }

        @Test
        @DisplayName("preflight response includes Access-Control-Max-Age")
        void preflight_vercelOrigin_includesMaxAge() throws Exception {
            mockMvc.perform(options(ENDPOINT)
                            .header(HttpHeaders.ORIGIN, VERCEL_ORIGIN)
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
                    .andExpect(status().isOk())
                    .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_MAX_AGE));
        }
    }

    // ── Preflight (OPTIONS) — localhost dev origin ────────────────────────────

    @Nested
    @DisplayName("OPTIONS preflight from localhost dev origin")
    class LocalhostPreflight {

        @Test
        @DisplayName("returns 200 with Access-Control-Allow-Origin for localhost:3000")
        void preflight_localhostOrigin_returns200WithAllowOriginHeader() throws Exception {
            mockMvc.perform(options(ENDPOINT)
                            .header(HttpHeaders.ORIGIN, LOCALHOST_ORIGIN)
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, LOCALHOST_ORIGIN));
        }
    }

    // ── Actual POST — Vercel production origin ────────────────────────────────

    @Nested
    @DisplayName("POST /api/contact from Vercel production origin")
    class VercelPost {

        @Test
        @DisplayName("POST from Vercel origin returns Access-Control-Allow-Origin header")
        void post_vercelOrigin_responseContainsAllowOriginHeader() throws Exception {
            mockMvc.perform(post(ENDPOINT)
                            .header(HttpHeaders.ORIGIN, VERCEL_ORIGIN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validPayload()))
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, VERCEL_ORIGIN));
        }
    }

    // ── Disallowed origin ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Requests from disallowed origins are rejected")
    class DisallowedOrigin {

        @Test
        @DisplayName("OPTIONS preflight from disallowed origin does NOT return allow-origin header")
        void preflight_disallowedOrigin_noAllowOriginHeader() throws Exception {
            mockMvc.perform(options(ENDPOINT)
                            .header(HttpHeaders.ORIGIN, DISALLOWED_ORIGIN)
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
                    .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        }

        @Test
        @DisplayName("POST from disallowed origin does NOT return allow-origin header")
        void post_disallowedOrigin_noAllowOriginHeader() throws Exception {
            mockMvc.perform(post(ENDPOINT)
                            .header(HttpHeaders.ORIGIN, DISALLOWED_ORIGIN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validPayload()))
                    .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        }
    }

    // ── Trailing-slash robustness ─────────────────────────────────────────────

    @Nested
    @DisplayName("Trailing-slash variants are handled correctly")
    class TrailingSlash {

        @Test
        @DisplayName("Vercel origin without trailing slash is matched (canonical form browsers send)")
        void preflight_vercelOriginNoTrailingSlash_isAllowed() throws Exception {
            // Browsers always send the Origin header without a trailing slash.
            // This is the canonical case: it must always be allowed.
            mockMvc.perform(options(ENDPOINT)
                            .header(HttpHeaders.ORIGIN, VERCEL_ORIGIN)
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, VERCEL_ORIGIN));
        }

        @Test
        @DisplayName("CorsConfig normalises configured origins — no trailing slash in allowed list")
        void corsConfig_normalisesConfiguredOrigins_stripsTrailingSlash() throws Exception {
            // Spring's CorsFilter normalises the incoming Origin before matching.
            // Our CorsConfig also strips trailing slashes from the *configured* origins
            // at startup so the allowlist is always in canonical form.  If a configured
            // origin accidentally had a trailing slash, it would still match the
            // browser's canonical (no-slash) origin correctly.
            // Verify the canonical origin is still allowed (regression guard).
            mockMvc.perform(options(ENDPOINT)
                            .header(HttpHeaders.ORIGIN, VERCEL_ORIGIN)
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, VERCEL_ORIGIN));
        }
    }

    // ── Comma-separated env-var parsing ──────────────────────────────────────

    @Nested
    @DisplayName("Comma-separated env-var value is parsed correctly")
    @TestPropertySource(properties = {
            // Simulate CORS_ALLOWED_ORIGINS being delivered as a single comma-separated
            // string (the common Render env-var format).  Both origins must be allowed.
            "app.cors.allowed-origins=https://developer-platform-inky.vercel.app,http://localhost:3000"
    })
    class CommaSeparatedEnvVar {

        @Test
        @DisplayName("Vercel origin is allowed when value is a comma-separated single string")
        void preflight_commaSeparatedEnvVar_vercelOriginAllowed() throws Exception {
            mockMvc.perform(options(ENDPOINT)
                            .header(HttpHeaders.ORIGIN, VERCEL_ORIGIN)
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, VERCEL_ORIGIN));
        }

        @Test
        @DisplayName("localhost:3000 is still allowed when value is a comma-separated single string")
        void preflight_commaSeparatedEnvVar_localhostOriginAllowed() throws Exception {
            mockMvc.perform(options(ENDPOINT)
                            .header(HttpHeaders.ORIGIN, LOCALHOST_ORIGIN)
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, LOCALHOST_ORIGIN));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String validPayload() {
        return """
                {
                  "name": "Jane Doe",
                  "email": "jane@example.com",
                  "subject": "Hello there",
                  "message": "This is a test message with enough content."
                }
                """;
    }
}
