FROM postgres:14.3-alpine

# CVE-2024-58251
RUN apk update && \
    apk add --no-cache --upgrade busybox && \
    rm -rf /var/cache/apk/*

COPY init-db.sh /docker-entrypoint-initdb.d

USER postgres

HEALTHCHECK --interval=10s --timeout=10s --retries=10 CMD psql -c 'select 1' -d idam -U ${DB_USERNAME}

EXPOSE 5432