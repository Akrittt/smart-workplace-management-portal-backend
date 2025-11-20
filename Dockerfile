# Stage 1: Build the application
FROM maven:3.9.5-amazoncorretto-17 AS build
WORKDIR /app

# avoid re-downloading deps on code change
COPY pom.xml .
RUN mvn -B -q -DskipTests dependency:go-offline

# copy sources and build
COPY src ./src
RUN mvn -B -DskipTests package

# Allow passing JAR name from build time (fallback to the expected artifact)
ARG JAR_FILE=target/Smart-Workplace-Management-Portal-0.0.1-SNAPSHOT.jar

# Stage 2: Run image (smaller, but ensure CA certs are present)
FROM eclipse-temurin:17-jre-alpine

# Install CA certs so SSL (Supabase) works; keep image small
RUN apk add --no-cache ca-certificates bash
# (optional) add curl for healthchecks:
# RUN apk add --no-cache curl

# Create a non-root user for security
RUN addgroup -S app && adduser -S -G app app
USER app

WORKDIR /home/app

# Copy built jar from builder stage
COPY --from=build --chown=app:app ${JAR_FILE} app.jar

# Allow Render to pass a port; fallback 8080 locally
ENV SERVER_PORT=${PORT:-8080}
ENV JAVA_OPTS=" -Xms256m -Xmx512m "

EXPOSE 8080

# Use exec form so signals are forwarded correctly; allow overriding CMD
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /home/app/app.jar --server.port=${SERVER_PORT}"]
