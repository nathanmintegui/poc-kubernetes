# Etapa 1: Build com JDK 21
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN sudo ./mvnw dependency:go-offline -B
COPY src ./src
RUN sudo ./mvnw clean package -DskipTests

# Etapa 2: Runtime com JRE 21 (mais leve)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

ENTRYPOINT ["sh", "-c", "java -jar app.jar"]
