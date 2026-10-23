FROM amazoncorretto:25-alpine@sha256:19f1e2198abaaf201f5b9faa39222412da3fad66415e9dfe253bd6763415097e

# Temporary fix for CVE-2026-91403
# Remove once the base image includes Corretto 25.0.5.0.9+
RUN apk add --no-cache --upgrade 'amazon-corretto-25=25.0.5.0.9-r0'

# CVE-2024-58251
RUN apk update && \
    apk add --no-cache --upgrade openssl busybox zlib && \
    rm -rf /var/cache/apk/*

# Create a group and user to run the application
RUN addgroup --gid 10000 appgroup && \
    adduser --uid 10000 --ingroup appgroup --disabled-password --gecos "" appuser

# Define a volume to safely store temporary files across restarts
VOLUME /tmp
WORKDIR /app

# Copy the JAR from the build output to the container
COPY build/libs/laa-landing-page.jar /app/application.jar

# Set the user to run the application
RUN chown -R appuser:appgroup /app
USER 10000

# Expose the application port
EXPOSE 8080

# Set the default command to run the application
CMD ["java", "-jar", "application.jar"]
