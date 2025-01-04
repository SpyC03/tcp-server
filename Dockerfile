FROM gradle:8.4-jdk17 AS builder

WORKDIR /app
COPY build.gradle settings.gradle ./
COPY src ./src
RUN gradle clean shadowJar -x test

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the JAR from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Create logs directory and set permissions
RUN mkdir /app/logs
RUN chmod 777 /app/logs

COPY config.properties .

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

ENTRYPOINT ["java", "-server", "-jar", "-Dfile.encoding=UTF-8", "-Xms2G", "-Xmx2G", "app.jar"]