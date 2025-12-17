FROM gradle:8.10.2-jdk21 AS builder
WORKDIR /app

COPY build.gradle .
COPY settings.gradle .
RUN gradle wrapper
RUN ./gradlew dependencies --no-daemon
COPY src src
RUN ./gradlew build --no-daemon -x test
RUN rm -rf /app/build/libs/*-plain.jar

FROM container-registry.oracle.com/graalvm/jdk:21
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

# prod profile
# ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]