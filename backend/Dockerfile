# --- Build stage: compiles the jar with Maven ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy just the POM first so dependency resolution is cached across builds
# that only change source files, not dependencies.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src src
RUN mvn -B clean package -DskipTests

# --- Runtime stage: a slim JRE with just the built jar ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Render injects PORT at runtime; application.properties already reads
# server.port=${PORT:8080}, so nothing else is needed here.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
