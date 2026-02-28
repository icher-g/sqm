FROM eclipse-temurin:21-jre

WORKDIR /opt/sqm

ARG JAR_FILE=sqm-middleware-mcp/target/sqm-middleware-mcp-0.3.0-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java","-jar","/opt/sqm/app.jar"]

