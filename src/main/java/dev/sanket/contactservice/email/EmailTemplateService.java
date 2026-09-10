package dev.sanket.contactservice.email;

import dev.sanket.contactservice.dto.ContactRequest;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Builds email content (HTML + plain-text) for contact form notifications.
 *
 * All visitor-supplied values are HTML-escaped before insertion into the
 * HTML template, preventing any injection via name/subject/message fields.
 *
 * Plain-text fallback is included for email clients that don't render HTML.
 */
@Service
public class EmailTemplateService {

    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss 'UTC'");

    public record EmailContent(String html, String text) {}

    /**
     * Builds both the HTML and plain-text versions of the contact notification.
     *
     * @param request the validated contact form submission
     * @return an {@link EmailContent} record containing both representations
     */
    public EmailContent buildContactEmail(ContactRequest request) {
        String timestamp = ZonedDateTime.now(ZoneOffset.UTC).format(TIMESTAMP_FMT);
        return new EmailContent(
                buildHtml(request, timestamp),
                buildPlainText(request, timestamp)
        );
    }

    // ── HTML ──────────────────────────────────────────────────────────────────

    private String buildHtml(ContactRequest request, String timestamp) {
        String name    = escape(request.name());
        String email   = escape(request.email());
        String subject = escape(request.subject());
        String message = escapeAndPreserveNewlines(request.message());
        String replyTo = escape(request.email());

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  <title>New Portfolio Contact</title>
                </head>
                <body style="margin:0;padding:0;background-color:#0a0a0f;font-family:'Segoe UI',Arial,sans-serif;">
                
                  <!-- Outer wrapper -->
                  <table width="100%%" cellpadding="0" cellspacing="0" border="0"
                         style="background-color:#0a0a0f;padding:40px 16px;">
                    <tr>
                      <td align="center">
                
                        <!-- Card -->
                        <table width="600" cellpadding="0" cellspacing="0" border="0"
                               style="max-width:600px;width:100%%;background-color:#0f0f1a;
                                      border-radius:12px;overflow:hidden;
                                      border:1px solid #1e1e3a;">
                
                          <!-- ── Header ── -->
                          <tr>
                            <td style="background:linear-gradient(135deg,#0d1b2a 0%%,#0a0a1f 50%%,#0d0a1f 100%%);
                                        padding:40px 40px 32px;text-align:center;
                                        border-bottom:2px solid #00d4ff;">
                              <!-- Accent line above title -->
                              <div style="display:inline-block;width:48px;height:3px;
                                          background:linear-gradient(90deg,#00d4ff,#7c3aed);
                                          border-radius:2px;margin-bottom:20px;"></div>
                              <h1 style="margin:0;font-size:26px;font-weight:700;
                                         color:#ffffff;letter-spacing:0.5px;">
                                New Portfolio Contact
                              </h1>
                              <p style="margin:10px 0 0;font-size:14px;
                                        color:#8892b0;letter-spacing:0.3px;">
                                Someone reached out through your portfolio.
                              </p>
                            </td>
                          </tr>
                
                          <!-- ── Details card ── -->
                          <tr>
                            <td style="padding:32px 40px 0;">
                              <table width="100%%" cellpadding="0" cellspacing="0" border="0"
                                     style="background-color:#13132a;border-radius:8px;
                                            border:1px solid #1e1e3a;overflow:hidden;">
                
                                <!-- Name -->
                                <tr>
                                  <td style="padding:14px 20px;border-bottom:1px solid #1e1e3a;
                                             width:120px;vertical-align:top;">
                                    <span style="font-size:11px;font-weight:600;
                                                 color:#00d4ff;text-transform:uppercase;
                                                 letter-spacing:1px;">Name</span>
                                  </td>
                                  <td style="padding:14px 20px;border-bottom:1px solid #1e1e3a;
                                             vertical-align:top;">
                                    <span style="font-size:14px;color:#ccd6f6;font-weight:500;">
                                      %s
                                    </span>
                                  </td>
                                </tr>
                
                                <!-- Email -->
                                <tr>
                                  <td style="padding:14px 20px;border-bottom:1px solid #1e1e3a;
                                             vertical-align:top;">
                                    <span style="font-size:11px;font-weight:600;
                                                 color:#00d4ff;text-transform:uppercase;
                                                 letter-spacing:1px;">Email</span>
                                  </td>
                                  <td style="padding:14px 20px;border-bottom:1px solid #1e1e3a;
                                             vertical-align:top;">
                                    <a href="mailto:%s"
                                       style="font-size:14px;color:#7c3aed;
                                              text-decoration:none;font-weight:500;">
                                      %s
                                    </a>
                                  </td>
                                </tr>
                
                                <!-- Subject -->
                                <tr>
                                  <td style="padding:14px 20px;border-bottom:1px solid #1e1e3a;
                                             vertical-align:top;">
                                    <span style="font-size:11px;font-weight:600;
                                                 color:#00d4ff;text-transform:uppercase;
                                                 letter-spacing:1px;">Subject</span>
                                  </td>
                                  <td style="padding:14px 20px;border-bottom:1px solid #1e1e3a;
                                             vertical-align:top;">
                                    <span style="font-size:14px;color:#ccd6f6;font-weight:500;">
                                      %s
                                    </span>
                                  </td>
                                </tr>
                
                                <!-- Sent at -->
                                <tr>
                                  <td style="padding:14px 20px;vertical-align:top;">
                                    <span style="font-size:11px;font-weight:600;
                                                 color:#00d4ff;text-transform:uppercase;
                                                 letter-spacing:1px;">Sent at</span>
                                  </td>
                                  <td style="padding:14px 20px;vertical-align:top;">
                                    <span style="font-size:14px;color:#8892b0;">%s</span>
                                  </td>
                                </tr>
                
                              </table>
                            </td>
                          </tr>
                
                          <!-- ── Message section ── -->
                          <tr>
                            <td style="padding:24px 40px 0;">
                              <p style="margin:0 0 10px;font-size:11px;font-weight:600;
                                        color:#00d4ff;text-transform:uppercase;letter-spacing:1px;">
                                Message
                              </p>
                              <div style="background-color:#13132a;border-radius:8px;
                                          border:1px solid #1e1e3a;
                                          border-left:3px solid #7c3aed;
                                          padding:20px 24px;">
                                <p style="margin:0;font-size:15px;line-height:1.7;
                                          color:#ccd6f6;white-space:pre-wrap;">%s</p>
                              </div>
                            </td>
                          </tr>
                
                          <!-- ── CTA button ── -->
                          <tr>
                            <td style="padding:32px 40px 0;text-align:center;">
                              <a href="mailto:%s?subject=Re%%3A%%20%s"
                                 style="display:inline-block;padding:14px 36px;
                                        background:linear-gradient(135deg,#00d4ff,#7c3aed);
                                        color:#ffffff;font-size:14px;font-weight:600;
                                        text-decoration:none;border-radius:6px;
                                        letter-spacing:0.5px;">
                                ↩ Reply to %s
                              </a>
                            </td>
                          </tr>
                
                          <!-- ── Divider ── -->
                          <tr>
                            <td style="padding:32px 40px 0;">
                              <div style="height:1px;background:linear-gradient(90deg,
                                transparent,#1e1e3a,transparent);"></div>
                            </td>
                          </tr>
                
                          <!-- ── Footer ── -->
                          <tr>
                            <td style="padding:24px 40px 32px;text-align:center;">
                              <p style="margin:0;font-size:12px;color:#4a5568;letter-spacing:0.3px;">
                                Sent from
                                <span style="color:#00d4ff;font-weight:600;">Sanket's Portfolio</span>
                                Contact Form
                              </p>
                            </td>
                          </tr>
                
                        </table>
                        <!-- /Card -->
                
                      </td>
                    </tr>
                  </table>
                  <!-- /Outer wrapper -->
                
                </body>
                </html>
                """.formatted(
                name,          // Name value
                replyTo,       // Email href
                email,         // Email display
                subject,       // Subject value
                timestamp,     // Sent at
                message,       // Message body
                replyTo,       // CTA mailto href
                subject,       // CTA subject param (already escaped)
                name           // CTA button label
        );
    }

    // ── Plain text ────────────────────────────────────────────────────────────

    private String buildPlainText(ContactRequest request, String timestamp) {
        return """
                NEW PORTFOLIO CONTACT
                ─────────────────────────────────────
                Someone reached out through your portfolio.

                Name    : %s
                Email   : %s
                Subject : %s
                Sent at : %s
                ─────────────────────────────────────

                MESSAGE:

                %s

                ─────────────────────────────────────
                Reply directly to this email to respond to %s.

                Sent from Sanket's Portfolio Contact Form
                """.formatted(
                request.name(),
                request.email(),
                request.subject(),
                timestamp,
                request.message(),
                request.name()
        );
    }

    // ── HTML escaping ─────────────────────────────────────────────────────────

    /**
     * Escapes the five unsafe HTML characters in a string.
     * Must be applied to every visitor-supplied value before HTML insertion.
     */
    static String escape(String value) {
        if (value == null) return "";
        return value
                .replace("&",  "&amp;")
                .replace("<",  "&lt;")
                .replace(">",  "&gt;")
                .replace("\"", "&quot;")
                .replace("'",  "&#x27;");
    }

    /**
     * Escapes HTML characters and converts newlines to {@code <br>} tags
     * so that multi-line messages render correctly in the HTML template.
     */
    static String escapeAndPreserveNewlines(String value) {
        return escape(value).replace("\n", "<br />");
    }
}
