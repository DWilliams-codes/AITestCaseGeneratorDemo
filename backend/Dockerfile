FROM maven:3.9.11-eclipse-temurin-21-alpine AS build
WORKDIR /workspace
COPY pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine-3.23
RUN apk add --no-cache curl \
    && apk upgrade --no-cache libexpat p11-kit p11-kit-trust \
    && addgroup -S testforge \
    && adduser -S testforge -G testforge
WORKDIR /app
COPY --from=build --chown=testforge:testforge /workspace/target/testforge-backend.jar app.jar
USER testforge
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/urandom", "-jar", "/app/app.jar"]
