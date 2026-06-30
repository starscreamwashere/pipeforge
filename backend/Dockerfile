# ─── Build stage ──────────────────────────────────────────
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Cache dependencies first (changes less often than source).
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -q -DskipTests clean package

# ─── Runtime stage ────────────────────────────────────────
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Run as a non-root user.
RUN useradd --system --uid 1001 pipeforge
COPY --from=build /app/target/pipeforge-0.0.1-SNAPSHOT.jar app.jar
USER pipeforge

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
