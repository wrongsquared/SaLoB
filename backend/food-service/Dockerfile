FROM eclipse-temurin:25-jre

# Step 2: Define the location of the JAR created by Gradle
# We use a wildcard to avoid issues with version numbers (0.0.1-SNAPSHOT)
ARG JAR_FILE=build/libs/*.jar

# Step 3: Copy the JAR from your host machine into the container image
COPY ${JAR_FILE} app.jar

# Step 4: Run the application
ENTRYPOINT ["java", "-jar", "/app.jar"]
