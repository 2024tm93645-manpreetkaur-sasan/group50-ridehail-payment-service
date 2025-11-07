# Stage 1 - Build the JAR using Gradle
FROM gradle:8.10-jdk21 AS builder
WORKDIR /app
COPY . .
RUN gradle clean bootJar --no-daemon

# Stage 2 - Run the JAR
FROM eclipse-temurin:25-jdk-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar payment-service.jar
EXPOSE 4100
ENTRYPOINT ["java", "-jar", "/app/payment-service.jar"]
