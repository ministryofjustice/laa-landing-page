FROM postgres:14.3-alpine@sha256:5973fec8d8284c8961ac3c23945e52d533dd0d21cc7b3c7dd7a46fd199bed4a4

# CVE-2024-58251
RUN apk update && \
    apk add --no-cache --upgrade busybox && \
    rm -rf /var/cache/apk/*

COPY init-db.sh /docker-entrypoint-initdb.d

USER postgres

HEALTHCHECK --interval=10s --timeout=10s --retries=10 CMD psql -c 'select 1' -d idam -U ${DB_USERNAME}

EXPOSE 5432