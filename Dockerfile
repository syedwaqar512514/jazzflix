# Multi-stage build for Jazz Auth Service
# Stage 1: Build stage
FROM eclipse-temurin:23-jdk-alpine AS builder

WORKDIR /app

# Copy pom.xml and download dependencies (for better layer caching)
COPY pom.xml .
RUN apk add --no-cache maven && \
    mvn dependency:resolve

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -U

# Stage 2: Runtime stage
FROM eclipse-temurin:23-jre-alpine

WORKDIR /app

# Install curl for health checks
RUN apk add --no-cache curl

# Copy JAR from builder stage
COPY --from=builder /app/target/auth-service-*.jar auth-service.jar

# Create a non-root user for security
RUN adduser -D -u 1000 appuser && chown -R appuser:appuser /app

RUN mkdir -p /mnt
RUN chown -R appuser:appuser /mnt

USER appuser

# Expose port
EXPOSE 8088

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8088/jazz/v1/api/auth/health || exit 1

# Application entry point
ENTRYPOINT ["java", "-jar", "jazz-flix.jar"]
