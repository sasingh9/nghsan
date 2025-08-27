# Use an official OpenJDK 11 runtime as a parent image, as specified in pom.xml
FROM openjdk:11-jre-slim

# Set the working directory in the container
WORKDIR /app

# Copy the executable JAR file from the target directory into the container
COPY target/trade-manager-0.0.1-SNAPSHOT.jar app.jar

# Expose port 8080 to allow communication with the application
EXPOSE 8080

# Run the application when the container launches
ENTRYPOINT ["java","-jar","app.jar"]
