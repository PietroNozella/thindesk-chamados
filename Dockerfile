FROM eclipse-temurin:21-jdk AS build
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline
COPY src src
RUN ./mvnw -B -ntp clean package

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
COPY --from=build /build/target/thindesk-1.0-alpha.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
