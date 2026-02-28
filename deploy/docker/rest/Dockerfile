FROM eclipse-temurin:21-jre

WORKDIR /opt/sqm

ARG JAR_FILE=sqm-middleware-rest/target/sqm-middleware-rest-0.3.0-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","/opt/sqm/app.jar"]

