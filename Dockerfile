# Stage 1: Build the application
# Use a JDK image to compile the Java code
FROM maven:3.9.5-amazoncorretto-17 AS build

# Set the working directory inside the container
WORKDIR /app

# Copy project files and download dependencies
COPY pom.xml .
# Copy the project source code
COPY src ./src

# Build the JAR file (skipping tests for speed)
RUN mvn clean install -DskipTests

# Stage 2: Create the final, lean runtime image
# Use a smaller JRE (Java Runtime Environment) for the final image to save size
FROM eclipse-temurin:17-jre-alpine

# Copy the built JAR file from the build stage
COPY --from=build /app/target/Smart-Workplace-Management-Portal-0.0.1-SNAPSHOT.jar app.jar

# Expose the default Spring Boot port
EXPOSE 8080

# Define the command to run the application
ENTRYPOINT ["java", "-jar", "/app.jar"]