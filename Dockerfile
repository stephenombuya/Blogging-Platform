# ---- Build Stage ----
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
# Download dependencies first (layer caching)
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn clean package -DskipTests -q

# ---- Runtime Stage ----
FROM eclipse-temurin:17-jre-jammy
LABEL maintainer="Blog Platform Team <support@blogplatform.com>"
LABEL version="1.0.0"

# Non-root user for security
RUN groupadd -r blogapp && useradd -r -g blogapp blogapp

WORKDIR /app

# Copy JAR from build stage
COPY --from=builder /app/target/blogging-platform-1.0.0.jar app.jar

# Create upload and log directories
RUN mkdir -p uploads logs && chown -R blogapp:blogapp /app

USER blogapp

EXPOSE 8080

# JVM tuning for containers
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseContainerSupport -XX:MaxRAMPercentage=70.0 -Djava.security.egd=file:/dev/./urandom"

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
