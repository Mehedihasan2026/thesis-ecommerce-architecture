FROM eclipse-temurin:17-jdk AS builder
WORKDIR /workspace
COPY . .
RUN chmod +x gradlew && ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /workspace/build/libs/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
