# --- Build Stage ---
# Use a Maven image with OpenJDK 11 to build the application
FROM maven:3.8.5-openjdk-11 AS build
WORKDIR /workspace
# Copy pom.xml first to leverage Docker cache for dependencies
COPY pom.xml .
RUN mvn dependency:go-offline
# Copy the rest of the source code
COPY src ./src
# Package the application
RUN mvn package -DskipTests

# --- Runtime Stage ---
# Use a slim JRE for a smaller final image
FROM openjdk:11-jre-slim
WORKDIR /app
# Copy the built JAR from the build stage
COPY --from=build /workspace/target/trade-manager-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
