# Portfolio Contact Service

Spring Boot microservice that powers the contact form on [sanket.dev](https://sanket.dev).

Receives `POST /api/contact` submissions, validates the payload, and sends a
notification email to the site owner via Gmail SMTP. The visitor's email is set
as the `Reply-To` header so replies go directly to them.

---

## Tech stack

| Layer | Technology |
|---|---|
| Runtime | Java 21, Spring Boot 3.3.4 |
| Email | Spring Mail + Gmail SMTP (TLS) |
| Validation | Jakarta Bean Validation |
| Tests | JUnit 5, Mockito, AssertJ, MockMvc |

---

## Local setup

### 1. Create a Gmail App Password

Google requires an **App Password** (not your regular Gmail password) when
2-Step Verification is enabled, which it must be for SMTP to work.

1. Go to your Google Account → **Security**
2. Under *How you sign in to Google*, open **2-Step Verification** (enable it if not already active)
3. At the bottom of the 2-Step Verification page, click **App passwords**
4. Choose app: *Mail*, device: *Other* → name it `Portfolio Contact Service`
5. Copy the 16-character password shown — you will not see it again

> Never paste this password into any source file or commit it to Git.

---

### 2. Set environment variables

Export these in your shell before running the application (or add them to a
`.env` file — it is git-ignored):

```bash
export MAIL_USERNAME=you@gmail.com          # your Gmail address
export MAIL_PASSWORD=xxxx-xxxx-xxxx-xxxx    # the 16-char App Password from step 1
export CONTACT_RECIPIENT_EMAIL=you@gmail.com # inbox that receives contact emails
```

You can also scope them to a single run:

```bash
MAIL_USERNAME=you@gmail.com \
MAIL_PASSWORD=xxxx-xxxx-xxxx-xxxx \
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

Check your Gmail inbox — you should receive the notification within seconds.

---

## Environment variables reference

| Variable | Required | Description |
|---|---|---|
| `MAIL_USERNAME` | Yes | Gmail address used to authenticate with SMTP |
| `MAIL_PASSWORD` | Yes | Gmail App Password (16 characters, no spaces) |
| `CONTACT_RECIPIENT_EMAIL` | Yes | Inbox that receives contact form notifications |
| `CORS_ALLOWED_ORIGINS` | No | Comma-separated allowed origins (default: `http://localhost:3000`) |
| `SERVER_PORT` | No | HTTP port (default: `8080`; local profile overrides to `8081`) |

---

## Running tests

```bash
mvn clean verify
```

All tests mock the email sender — no real emails are sent during the test suite.

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
| `200 OK` | Email sent successfully |
| `400 Bad Request` | Validation failure — `fieldErrors` map included in response |
| `500 Internal Server Error` | SMTP delivery failed — safe message returned, no credentials exposed |
