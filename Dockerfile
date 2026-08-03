# Build stage: compiles the app so the host does not need Maven or a JDK.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy the descriptor first so dependency resolution is cached separately from
# source changes, which keeps rebuilds after a code edit fast.
COPY pom.xml .
RUN mvn -B -q dependency:resolve || true

COPY src ./src
RUN mvn -B -DskipTests package

# Runtime stage: only a JRE and the packaged jar, so the image stays small.
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/JavaQuiz-1.0-SNAPSHOT.jar app.jar

EXPOSE 8080

# MaxRAMPercentage keeps the heap inside a small container memory limit, which
# the JVM would otherwise overshoot and get killed for.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
