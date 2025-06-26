FROM eclipse-temurin:17-jre
WORKDIR /app

COPY libs/opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar
COPY build/server.jar /app/server.jar