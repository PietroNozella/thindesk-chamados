FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
ENV JAVA_TOOL_OPTIONS="-XX:InitialRAMPercentage=12.5 -XX:MaxRAMPercentage=50.0 -XX:+ExitOnOutOfMemoryError"
COPY --from=build /build/target/*.jar ./app.jar
COPY scripts/iniciar-container.sh ./iniciar-container.sh
RUN chmod 755 /app/iniciar-container.sh
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["/app/iniciar-container.sh"]
