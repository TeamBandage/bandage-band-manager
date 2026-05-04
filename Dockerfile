# syntax=docker/dockerfile:1.7

# ============================================================
# Build stage — Amazon Corretto 21 (Alpine)
# ============================================================
FROM amazoncorretto:21-alpine AS builder

WORKDIR /workspace

COPY gradle ./gradle
COPY gradlew settings.gradle.kts build.gradle.kts ./
RUN chmod +x ./gradlew \
 && ./gradlew dependencies --no-daemon

COPY src ./src
RUN ./gradlew clean bootJar -x test --no-daemon \
 && cp build/libs/*-SNAPSHOT.jar /workspace/app.jar

# /workspace/extracted/app.jar 파일 & /workspace/extracted/lib/ 폴더 생성
RUN java -Djarmode=tools -jar /workspace/app.jar extract --destination /workspace/extracted

# ============================================================
# Runtime stage — Amazon Corretto 21 JRE (초경량 런타임 환경)
# ============================================================
FROM amazoncorretto:21-alpine-jre AS runtime

RUN apk add --no-cache tzdata curl \
 && cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime \
 && echo "Asia/Seoul" > /etc/timezone

RUN addgroup -S appgroup \
 && adduser -S appuser -G appgroup -h /app -s /sbin/nologin

WORKDIR /app

# 외부 라이브러리 먼저 복사 (도커 캐시 타겟)
COPY --from=builder --chown=appuser:appgroup /workspace/extracted/lib/ ./lib/
# 소스 코드 jar 파일 복사
COPY --from=builder --chown=appuser:appgroup /workspace/extracted/app.jar ./app.jar

USER appuser

ENV PROFILE_ACTIVE=dev \
    SERVER_PORT=8080 \
    TZ=Asia/Seoul \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -fsS "http://localhost:${SERVER_PORT}/actuator/health" || exit 1

# java -jar 실행
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dspring.profiles.active=$PROFILE_ACTIVE -jar app.jar"]