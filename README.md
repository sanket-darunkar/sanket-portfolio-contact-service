# Portfolio Contact Service

Spring Boot microservice that powers the contact form on [sanket.dev](https://sanket.dev).

Receives `POST /api/contact` submissions, validates the payload, and sends a
notification email to the site owner via the [Resend](https://resend.com) HTTP
Email API. The visitor's email is set as `reply_to` so replies go directly to
them. No SMTP connection is required — email is delivered over HTTPS.

---

## Tech stack

| Layer | Technology |
|---|---|
| Runtime | Java 21, Spring Boot 3.3.4 |
| Email | Resend HTTP Email API (`java.net.http.HttpClient`) |
| Validation | Jakarta Bean Validation |
| Tests | JUnit 5, Mockito, AssertJ, MockMvc |

---

## Local setup

### 1. Create a Resend account and API key

1. Sign up at [resend.com](https://resend.com)
2. Go to **API Keys** → **Create API Key**
3. Copy the key (starts with `re_`) — you will not see it again
4. For the sender address:
   - During development / testing you can use `onboarding@resend.dev`
     (Resend's shared testing address — no domain verification needed)
   - For production set `RESEND_FROM_EMAIL` to an address on a domain you
     have verified in the Resend dashboard

> Never paste your API key into any source file or commit it to Git.

---

### 2. Set environment variables

Export these in your shell before running the application (or add them to a
`.env` file — it is git-ignored):

```bash
export RESEND_API_KEY=re_xxxxxxxxxxxxxxxxxxxx        # Resend API key
export RESEND_FROM_EMAIL=onboarding@resend.dev       # verified sender address
export CONTACT_RECIPIENT_EMAIL=you@gmail.com         # inbox that receives contact emails
```

You can also scope them to a single run:

```bash
RESEND_API_KEY=re_xxxxxxxxxxxxxxxxxxxx \
RESEND_FROM_EMAIL=onboarding@resend.dev \
CONTACT_RECIPIENT_EMAIL=you@gmail.com \
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

---

### 3. Run the application

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

The service starts on **http://localhost:8081**.

---

### 4. Test the endpoint

```bash
curl -i -X POST http://localhost:8081/api/contact \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Your Name",
    "email": "visitor@example.com",
    "subject": "Hello from the contact form",
    "message": "This is a test submission from the local dev environment."
  }'
```

Expected response (`200 OK`):

```json
{
  "success": true,
  "message": "Your message has been received. I'll get back to you soon."
}
```

Check your inbox — you should receive the Resend notification within seconds.

---

## Environment variables reference

| Variable | Required | Description |
|---|---|---|
| `RESEND_API_KEY` | Yes | Resend API secret key (starts with `re_`) |
| `RESEND_FROM_EMAIL` | Yes | Verified sender address (e.g. `onboarding@resend.dev` for testing) |
| `CONTACT_RECIPIENT_EMAIL` | Yes | Inbox that receives contact form notifications |
| `CORS_ALLOWED_ORIGINS` | No | Comma-separated allowed origins (default includes Vercel + localhost) |
| `PORT` / `SERVER_PORT` | No | HTTP port (default: `8080`; local profile overrides to `8081`) |

---

## Render deployment

Ensure the following environment variables are set in your Render service dashboard:

| Variable | Value |
|---|---|
| `RESEND_API_KEY` | Your Resend API key |
| `RESEND_FROM_EMAIL` | Your verified sender (e.g. `onboarding@resend.dev`) |
| `CONTACT_RECIPIENT_EMAIL` | Your notification inbox |
| `CORS_ALLOWED_ORIGINS` | `https://developer-platform-inky.vercel.app` |

> `MAIL_USERNAME`, `MAIL_PASSWORD`, and `MAIL_HOST` are no longer used and
> can be removed from Render if they were previously set.

---

## Running tests

```bash
mvn clean verify
```

All tests mock the HTTP client — no real emails are sent and no network
connections are made during the test suite.

---

## API

### `POST /api/contact`

**Request body:**

```json
{
  "name": "string (2–100 chars, required)",
  "email": "string (valid email, required)",
  "subject": "string (2–150 chars, required)",
  "message": "string (10–5000 chars, required)"
}
```

**Responses:**

| Status | Meaning |
|---|---|
| `200 OK` | Email sent successfully via Resend |
| `400 Bad Request` | Validation failure — `fieldErrors` map included in response |
| `500 Internal Server Error` | Resend API delivery failed — safe message returned, no credentials exposed |
