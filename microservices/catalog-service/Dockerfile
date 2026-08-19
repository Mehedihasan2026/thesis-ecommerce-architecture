FROM eclipse-temurin:17-jdk AS builder
WORKDIR /workspace
COPY . .
RUN chmod +x gradlew && ./gradlew :catalog-service:bootJar --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /workspace/catalog-service/build/libs/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
