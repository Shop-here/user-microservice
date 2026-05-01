# Use official maintained Java image
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Copy built jar
COPY target/*.jar app.jar

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
