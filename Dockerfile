# Multi-stage Docker build for Faculty Leave Management System
# 1. Build stage
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy backend files
COPY backend/lib/ ./backend/lib/
COPY backend/src/ ./backend/src/

# Compile Java backend
RUN mkdir -p backend/bin && \
    javac -encoding UTF-8 -cp "backend/lib/*" -d backend/bin $(find backend/src -name "*.java")

# 2. Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy compiled classes and libraries
COPY --from=builder /app/backend/bin/ ./backend/bin/
COPY backend/lib/ ./backend/lib/

# Copy frontend static assets and config
COPY frontend/ ./frontend/
COPY db.properties ./db.properties

# Standard port configuration
ENV PORT=8080
EXPOSE 8080

# Run Java application
CMD ["java", "-cp", "backend/bin:backend/lib/*", "Main"]
