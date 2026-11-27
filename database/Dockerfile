FROM postgres:14.25-alpine@sha256:e7d9e6aac7fe3232f7661da103aebc0ce38b04105b46af5e9edd5ef6bc343f20

# CVE-2024-58251
RUN apk update && \
    apk add --no-cache --upgrade busybox && \
    rm -rf /var/cache/apk/*

COPY init-db.sh /docker-entrypoint-initdb.d

USER postgres

HEALTHCHECK --interval=10s --timeout=10s --retries=10 CMD psql -c 'select 1' -d idam -U ${DB_USERNAME}

EXPOSE 5432