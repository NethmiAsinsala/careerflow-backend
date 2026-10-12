FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml ./
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 appuser
COPY --from=build /build/target/careerflow-backend-1.0-SNAPSHOT.jar /app/backend.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/backend.jar"]
