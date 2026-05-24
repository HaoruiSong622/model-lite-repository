FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY target/model-lite-repository-*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=dev
ENV SERVER_PORT=8080

EXPOSE 8080

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-jar", "app.jar"]
