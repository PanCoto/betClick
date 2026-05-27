
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /build


COPY pom.xml .
RUN mvn dependency:go-offline -B --no-transfer-progress


COPY src ./src
RUN mvn clean package -DskipTests -B --no-transfer-progress


FROM eclipse-temurin:17-jre-jammy AS runtime


LABEL maintainer="betclick-team"
LABEL org.opencontainers.image.title="betClick"
LABEL org.opencontainers.image.description="Sports Betting Application — SBD Project"
LABEL org.opencontainers.image.version="1.0.0"


RUN groupadd --system betclick && useradd --system --gid betclick betclick

WORKDIR /app


COPY --from=builder /build/target/betclick-1.0.0.jar app.jar


RUN chown betclick:betclick app.jar

USER betclick


EXPOSE 8080


HEALTHCHECK \
  --interval=30s \
  --timeout=10s \
  --start-period=90s \
  --retries=4 \
  CMD wget --no-verbose --tries=1 --spider --no-check-certificate \
        https://localhost:8443/actuator/health || exit 1


ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
