# Start with a Java 17 base image
FROM eclipse-temurin:21-jre-alpine

# Create app directory
WORKDIR /app

# Copy the built jar from the build stage (or from local build context)
COPY build/libs/laa-new-portal-0.0.1-SNAPSHOT.jar app.jar

# Expose port 8080 (Spring Boot default)
EXPOSE 8080

# Run the application
ENTRYPOINT ["java","-jar","/app/app.jar"]