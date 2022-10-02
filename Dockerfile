FROM maven:latest AS build
COPY src /home/epapers/src
COPY pom.xml /home/epapers
RUN mvn -f /home/epapers/pom.xml install -DskipTests

FROM eclipse-temurin:11-alpine
COPY --from=build /home/epapers/target/*.jar ./epapers.jar
EXPOSE 8000:8000
ENTRYPOINT [ "java", "-jar", "-XX:MaxDirectMemorySize=2048M", "-Xmx1024m", "./epapers.jar" ]