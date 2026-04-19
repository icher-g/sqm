FROM node:22-alpine AS web-build

WORKDIR /workspace/sqm-playground-web

ARG VITE_PLAYGROUND_API_BASE_URL=/sqm/playground/api/v1
ENV VITE_PLAYGROUND_API_BASE_URL=${VITE_PLAYGROUND_API_BASE_URL}

COPY sqm-playground-web/package*.json ./
RUN npm ci

COPY sqm-playground-web/ ./

RUN npm run build

FROM maven:3.9.11-eclipse-temurin-21 AS app-build

WORKDIR /workspace

COPY . .

RUN mvn -B -ntp -pl sqm-playground-rest -am -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /opt/sqm

COPY --from=app-build /workspace/sqm-playground-rest/target/sqm-playground-rest-*.jar app.jar
COPY --from=web-build /workspace/sqm-playground-web/dist /opt/sqm/static

ENV SPRING_WEB_RESOURCES_STATIC_LOCATIONS=file:/opt/sqm/static/
ENV SERVER_PORT=8080

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/opt/sqm/app.jar"]
