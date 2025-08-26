# Stage 1: Build the React frontend
FROM node:18 AS frontend-builder
WORKDIR /app/frontend

# Copy package configuration and install dependencies
COPY frontend/package.json frontend/package-lock.json ./
RUN npm install

# Copy the rest of the frontend source code and build
COPY frontend/ ./
RUN npm run build

# Stage 2: Build the Spring Boot backend
FROM maven:3.8.5-openjdk-11 AS backend-builder
WORKDIR /app

# Copy the built frontend assets from the first stage
# This places the static files where Spring Boot's Maven plugin will find them
COPY --from=frontend-builder /app/frontend/build ./src/main/resources/static

# Copy the backend source code and build the JAR
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests

# Stage 3: Create the final lightweight image
FROM openjdk:11-jre-slim
WORKDIR /app

# Copy the executable JAR from the backend build stage
COPY --from=backend-builder /app/target/*.jar app.jar

# Expose the port the application runs on
EXPOSE 8080

# Command to run the application
CMD ["java", "-jar", "app.jar"]
