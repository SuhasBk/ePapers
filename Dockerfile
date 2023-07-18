FROM maven:3.8.3-amazoncorretto-17 AS build
COPY src /home/epapers/src
COPY pom.xml /home/epapers
RUN mvn -f /home/epapers/pom.xml -D skipTests package spring-boot:repackage

FROM amazoncorretto:17
COPY --from=build /home/epapers/target/*.jar ./epapers.jar
EXPOSE 8000:8000
ENTRYPOINT [ "java", "-XX:MaxDirectMemorySize=2048M", "-Xmx1024m", "-jar", "./epapers.jar" ]