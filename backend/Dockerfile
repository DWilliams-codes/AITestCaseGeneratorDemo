FROM maven:3.9.11-eclipse-temurin-21-alpine@sha256:922927df2c662cdd47ddb116443d6bec4696cfae3de1a0ddac8fcc7b87ce61ae AS build
WORKDIR /workspace
COPY pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine-3.23@sha256:3f08b13888f595cc49edabea7250ba69499ba25602b267da591720769400e08c
RUN apk add --no-cache curl \
    && apk upgrade --no-cache libexpat p11-kit p11-kit-trust \
    && addgroup -S testforge \
    && adduser -S testforge -G testforge
WORKDIR /app
COPY --from=build --chown=testforge:testforge /workspace/target/testforge-backend.jar app.jar
USER testforge
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/urandom", "-jar", "/app/app.jar"]
