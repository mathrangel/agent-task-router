# Stage 1 — build
# Maven + JDK 21 image, used only to compile the app. Discarded after this stage.
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom.xml first and resolve dependencies before copying source code.
# Docker caches each layer: as long as pom.xml doesn't change, this layer
# is reused on future builds even if application code changes.
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn package -DskipTests

# Stage 2 — runtime
# Minimal JRE-only image. No Maven, no compiler, no source code, no build cache.
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy only the built jar from the build stage.
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
