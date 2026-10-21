FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
COPY session-contract/build.gradle session-contract/build.gradle
COPY login-service/build.gradle login-service/build.gradle
RUN mkdir -p cart-service
RUN ./gradlew :login-service:dependencies --no-daemon
COPY . .
RUN ./gradlew :login-service:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/login-service/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
