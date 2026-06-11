# ==========================================
# Build Stage
# ==========================================
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy parent and modules POMs
COPY pom.xml .
COPY forgeauth-common/pom.xml forgeauth-common/
COPY forgeauth-domain/pom.xml forgeauth-domain/
COPY forgeauth-infrastructure/pom.xml forgeauth-infrastructure/
COPY forgeauth-server/pom.xml forgeauth-server/

# Download dependencies (caching layer)
RUN mvn dependency:go-offline -B -fn

# Copy source code
COPY forgeauth-common/src forgeauth-common/src
COPY forgeauth-domain/src forgeauth-domain/src
COPY forgeauth-infrastructure/src forgeauth-infrastructure/src
COPY forgeauth-server/src forgeauth-server/src

# Package the application (skip tests for production packaging)
RUN mvn clean package -DskipTests -B

# ==========================================
# Run Stage
# ==========================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create a non-root user for security
RUN addgroup -S forgeauth && adduser -S forgeauth -G forgeauth
USER forgeauth

# Copy final jar
COPY --from=build /app/forgeauth-server/target/forgeauth-server-1.0.0-SNAPSHOT.jar app.jar

# Expose Web Port
EXPOSE 8080

# Run the app
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
