# Build stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

# Copy parent POM and common module first for layer caching
COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY common/src common/src
COPY payment-service/pom.xml payment-service/pom.xml
COPY payment-service/src payment-service/src

# Build only the modules needed
RUN mvn clean package -pl common,payment-service -am -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S payflow && adduser -S payflow -G payflow
USER payflow

COPY --from=builder /workspace/payment-service/target/payment-service-*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
