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

# The slim Alpine JRE ships without the font libraries Java's 2D graphics needs. PDF
# downloads rasterize SVG logos (see PdfPhotoLoader), and an SVG containing text - or
# any Java2D font initialisation - fails with a "libfreetype"/fontconfig error without
# these. Small, standard fix for Java imaging on Alpine.
RUN apk add --no-cache fontconfig freetype ttf-dejavu

COPY --from=build /app/target/*.jar app.jar

# Render injects PORT at runtime; application.properties already reads
# server.port=${PORT:8080}, so nothing else is needed here.
EXPOSE 8080

# Explicit memory tuning matters on a small container. Without it, the JVM
# sizes heap as a percentage of container memory but leaves Metaspace (class
# metadata) completely uncapped by default - as more entities/services/
# controllers get added over time, Metaspace usage grows right along with
# them, with nothing stopping it from eating into the container's memory the
# heap is trying to share. -Xmx/-XX:MaxMetaspaceSize give both a hard, safe
# ceiling with headroom left for thread stacks and other native/JVM overhead.
#
# Sized for Render's Standard instance (2GB) - 1200m heap + 256m metaspace
# leaves ~590MB for thread stacks (server.tomcat.threads.max=100 in
# application-prod.properties, each reserving ~1MB) and other native/OS
# overhead. If this ever moves back to a smaller instance, scale these down
# together with that thread count - see this repo's README "Memory on
# Starter" callout for the OOM-kill symptom that shows up if they're too high
# for the container they're actually running in.
ENTRYPOINT ["java", "-Xmx1200m", "-XX:MaxMetaspaceSize=256m", "-XX:+UseContainerSupport", "-Djava.awt.headless=true", "-jar", "app.jar"]
