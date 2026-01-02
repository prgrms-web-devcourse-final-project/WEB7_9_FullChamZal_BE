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
RUN microdnf install -y tzdata && microdnf clean all
RUN ln -sf /usr/share/zoneinfo/Asia/Seoul /etc/localtime

COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-Dspring.profiles.active=prod", "-jar", "app.jar"]

# prod profile
# ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]