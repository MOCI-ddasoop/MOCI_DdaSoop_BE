FROM gradle:jdk-21-and-23-graal-jammy AS builder

WORKDIR /app

COPY build.gradle .
COPY settings.gradle .

RUN gradle dependencies --no-daemon

COPY .env .
COPY src src

RUN gradle build --no-daemon

FROM container-registry.oracle.com/graalvm/jdk:21

WORKDIR /app
COPY --from=builder /app/build/libs/*jar app.jar
COPY --from=builder /app/.env .env

ENTRYPOINT ["java", "-Dspring.profiles.active=dev", "-jar", "app.jar"]