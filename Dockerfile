FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY Backend/mvnw Backend/pom.xml ./
COPY Backend/.mvn ./.mvn

RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY Backend/src ./src

RUN ./mvnw clean package -DskipTests -B

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Dserver.port=$PORT -jar app.jar"]
