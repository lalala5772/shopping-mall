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
# MaxRAMPercentage는 컨테이너 메모리 상한(docker-compose.yml의 mem_limit) 기준으로 계산된다.
# 75%는 프리티어급 소형 인스턴스에서 메타스페이스/스레드스택 여유 없이 컨테이너가 바로 OOMKilled 될 수 있어 65%로 낮춘다.
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=65.0", "-jar", "app.jar"]
