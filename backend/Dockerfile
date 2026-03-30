FROM gradle:jdk17 AS cache

WORKDIR /app

COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./

RUN gradle dependencies --no-daemon --stacktrace

FROM gradle:jdk17 AS builder

WORKDIR /app

COPY --from=cache /app/.gradle ./.gradle
COPY --from=cache /app .

COPY src src
COPY mail mail

RUN gradle bootJar -x test --no-daemon --stacktrace

FROM eclipse-temurin:17-jre-jammy

RUN apt-get update \
 && apt-get install -y --no-install-recommends python3 python3-pip \
 && rm -rf /var/lib/apt/lists/*

RUN pip3 install --no-cache-dir exchangelib

WORKDIR /app
COPY --from=builder /app/build/libs/*.jar ./app.jar
COPY --from=builder /app/mail ./mail

CMD ["java", "-jar", "app.jar"]
