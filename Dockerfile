## RayyanMart — production Docker image (Spring Boot 2.7.18 WAR on Tomcat 9)
##
## Stage 1: build the WAR with Maven + JDK 17 (same command as the CI workflow)
## Stage 2: run it inside Tomcat 9 (javax.servlet), served at the app root.
##
## Why Tomcat 9? The WAR is packaged with spring-boot-starter-tomcat as
## "provided" and uses javax.* servlet APIs, so it must run on Tomcat 9 (not 10+).

# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package

# ---- Runtime stage ----
FROM tomcat:9.0-jdk17-temurin

# Deploy the WAR at the application root ("/").
COPY --from=build /app/target/online-shopping-system.war /usr/local/tomcat/webapps/ROOT.war

# Bind Tomcat's HTTP connector to Render's $PORT (default 10000).
# Tomcat ships with its connector hardcoded to 8080; server.servlet.port is
# ignored for WARs running under an external Tomcat, so we patch server.xml at
# startup using the PORT provided by Render.
COPY deploy/docker-entrypoint.sh /usr/local/bin/rayyanmart-entrypoint.sh

EXPOSE 8080
ENTRYPOINT ["/usr/local/bin/rayyanmart-entrypoint.sh"]