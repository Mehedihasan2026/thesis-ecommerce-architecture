FROM eclipse-temurin:17-jdk AS builder
WORKDIR /workspace
COPY . .
RUN chmod +x gradlew && ./gradlew :cart-service:bootJar --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /workspace/cart-service/build/libs/*.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
