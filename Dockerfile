# --- 빌드 단계 ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Gradle wrapper + 빌드 스크립트 먼저 복사 (의존성 캐싱)
COPY gradlew gradlew.bat ./
COPY gradle ./gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies || true

# 소스 복사 후 빌드
COPY src ./src
RUN ./gradlew --no-daemon bootJar -x test

# --- 런타임 단계 ---
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar

# Render/Railway는 PORT 환경변수를 주입 (기본 8080)
ENV SERVER_PORT=8080
EXPOSE 8080

ENTRYPOINT ["sh","-c","java -Dfile.encoding=UTF-8 -Dserver.port=${PORT:-${SERVER_PORT}} -jar app.jar"]
