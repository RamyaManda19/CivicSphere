
# ---------- Build Stage ----------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy Maven configuration
COPY pom.xml .

# Copy source code
COPY src ./src

# Download dependencies
RUN mvn dependency:go-offline -B

# Build project
RUN mvn clean package -DskipTests


# ---------- Runtime Stage ----------
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Copy compiled Java classes
COPY --from=build /app/target/classes ./classes

# Download required libraries
RUN apk add --no-cache curl && \
    mkdir -p lib && \
    curl -fL -o lib/servlet-api.jar \
    https://repo1.maven.org/maven2/org/apache/tomcat/embed/tomcat-embed-core/11.0.0/tomcat-embed-core-11.0.0.jar && \
    curl -fL -o lib/gson.jar \
    https://repo1.maven.org/maven2/com/google/code/gson/gson/2.9.0/gson-2.9.0.jar && \
    curl -fL -o lib/mysql-connector.jar \
    https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/9.1.0/mysql-connector-j-9.1.0.jar

# Configure classpath
ENV CP="classes:lib/servlet-api.jar:lib/gson.jar:lib/mysql-connector.jar"

# Render port
ENV PORT=10000

# Copy database properties if present
COPY src/main/resources/db.properties ./db.properties

# Start application
ENTRYPOINT ["sh", "-c", "java -cp \"${CP}\" com.civicsphere.CivicSphereServer ${PORT:-10000}"]
