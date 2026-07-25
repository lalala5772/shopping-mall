# ---- Build stage ----
FROM gradle:8.10-jdk17 AS build
WORKDIR /workspace
COPY build.gradle settings.gradle ./
COPY src src
RUN gradle bootJar --no-daemon -x test

# ---- Run stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build /workspace/build/libs/*.jar app.jar
RUN mkdir -p /app/uploads && chown -R spring:spring /app
USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
