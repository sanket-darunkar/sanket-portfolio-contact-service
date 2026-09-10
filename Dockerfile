# ─────────────────────────────────────────────────────────────────────────────
# Stage 1 — Build
# Uses the full Maven + JDK 21 image to compile and package the application.
# The target directory is never copied into the final image.
# ─────────────────────────────────────────────────────────────────────────────
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy dependency descriptor first so Docker can cache the dependency layer.
# Re-downloading dependencies only happens when pom.xml changes.
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source and build the fat JAR, skipping tests (tests run in CI, not here).
COPY src ./src
RUN mvn clean package -DskipTests -q

# ─────────────────────────────────────────────────────────────────────────────
# Stage 2 — Runtime
# Lightweight JRE-only image — no Maven, no JDK, no build tools.
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

# Non-root user for security — never run the app as root in production.
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy only the fat JAR produced in the build stage.
COPY --from=builder /build/target/contact-service-*.jar app.jar

# Transfer ownership to the non-root user.
RUN chown appuser:appgroup app.jar

USER appuser

# Render injects PORT at runtime; the application reads it via
# server.port=${PORT:${SERVER_PORT:8080}} in application.yml.
# We document 8080 as the default but do not enforce it with EXPOSE
# since the actual port is dynamic on Render.
EXPOSE 8080

# JVM flags:
#   -XX:+UseContainerSupport   — respect cgroup memory/CPU limits (default in JDK 11+, explicit for clarity)
#   -XX:MaxRAMPercentage=75.0  — cap heap at 75% of container RAM, leaving headroom for non-heap
#   -Djava.security.egd=...    — faster SecureRandom startup (important for SMTP/TLS init)
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
