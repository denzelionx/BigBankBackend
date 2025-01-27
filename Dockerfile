# Use a Maven image to build the application
FROM maven:3.9.4-eclipse-temurin-21 AS builder

# Set the working directory inside the container
WORKDIR /app

# Copy the entire project into the container
COPY . .
 
# Build the project using Maven
RUN mvn clean package -DskipTests
 
# Use a lightweight JDK image to run the application
FROM eclipse-temurin:21-jdk-jammy
 
# Set the working directory
WORKDIR /app
 
# Copy the JAR file from the builder stage
COPY --from=builder /app/target/*.jar app.jar
 
# Expose the port your Spring Boot application uses
EXPOSE 9090
 
# Set the entry point to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]