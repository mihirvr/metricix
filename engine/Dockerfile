# --- Stage 1: Build the JAR ---
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy gradle wrapper and source code
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src src

# Make the wrapper executable and build the application (skip tests for speed)
RUN chmod +x ./gradlew
RUN ./gradlew bootJar -x test

# --- Stage 2: The Minimal Production Image ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy ONLY the built JAR from the builder stage
COPY --from=builder /app/build/libs/*.jar metricix.jar

# Expose the port Netty will run on
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "metricix.jar"]