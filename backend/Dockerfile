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

# Explicit memory tuning matters on a small container (Render Starter = 512Mi).
# Without it, the JVM sizes heap as a percentage of container memory but leaves
# Metaspace (class metadata) completely uncapped by default - as more entities/
# services/controllers get added over time, Metaspace usage grows right along
# with them, with nothing stopping it from eating into the same 512Mi the heap
# is trying to share. -Xmx/-XX:MaxMetaspaceSize give both a hard, safe ceiling
# with headroom left for thread stacks and other native/JVM overhead.
ENTRYPOINT ["java", "-Xmx280m", "-XX:MaxMetaspaceSize=160m", "-XX:+UseContainerSupport", "-jar", "app.jar"]
