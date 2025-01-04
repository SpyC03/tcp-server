FROM gradle:8.4-jdk17 AS builder

WORKDIR /app
COPY build.gradle settings.gradle ./
COPY src ./src
RUN gradle clean shadowJar -x test

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

RUN mkdir /app/logs
RUN chmod 777 /app/logs

COPY run.sh ./run.sh
RUN chmod +x ./run.sh

COPY config.properties ./config.properties

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

ENTRYPOINT ["./run.sh"]

