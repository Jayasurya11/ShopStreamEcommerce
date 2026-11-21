# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-26 AS build
ARG MODULE
WORKDIR /w
COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn -q -B -pl ${MODULE} -am package -DskipTests

FROM eclipse-temurin:25-jre
ARG MODULE
LABEL org.opencontainers.image.title="shopstream-${MODULE}"
RUN groupadd --system --gid 10001 app && useradd --system --uid 10001 --gid app --no-create-home app
WORKDIR /app
COPY --from=build --chown=10001:10001 /w/${MODULE}/target/${MODULE}-1.0.0.jar app.jar
USER 10001:10001
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java", "-jar", "app.jar"]
