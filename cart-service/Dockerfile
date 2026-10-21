FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
COPY session-contract/build.gradle session-contract/build.gradle
COPY cart-service/build.gradle cart-service/build.gradle
RUN mkdir -p login-service
RUN ./gradlew :cart-service:dependencies --no-daemon
COPY . .
RUN ./gradlew :cart-service:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/cart-service/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
