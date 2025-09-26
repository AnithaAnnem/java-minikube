# Use OpenJDK 11 base image
FROM openjdk:17-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the Maven build jar into the container
# Make sure you build the jar first: mvn clean package
COPY target/config-server-1.0.0.jar app.jar

# Expose the port your config server runs on
EXPOSE 8888

# Run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]
