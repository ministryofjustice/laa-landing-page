FROM postgres:14.23-alpine@sha256:f1341c01408dc7278e9d365ed4f860cd3f87dd16b4464ac326fc0f422083a579

# CVE-2024-58251
RUN apk update && \
    apk add --no-cache --upgrade busybox && \
    rm -rf /var/cache/apk/*

COPY init-db.sh /docker-entrypoint-initdb.d

USER postgres

HEALTHCHECK --interval=10s --timeout=10s --retries=10 CMD psql -c 'select 1' -d idam -U ${DB_USERNAME}

EXPOSE 5432