FROM amazoncorretto:17
COPY JAR/*.jar ./epapers.jar
EXPOSE 8000:8000
ENTRYPOINT [ "java", "-XX:MaxDirectMemorySize=2048M", "-Xmx1024m", "-jar", "./epapers.jar" ]