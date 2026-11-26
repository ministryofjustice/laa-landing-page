FROM postgres:14.24-alpine@sha256:928ce526d67711997cc5c6949b55382d4e7cb559d981d5899df132e4936ec03e

# CVE-2024-58251
RUN apk update && \
    apk add --no-cache --upgrade busybox && \
    rm -rf /var/cache/apk/*

COPY init-db.sh /docker-entrypoint-initdb.d

USER postgres

HEALTHCHECK --interval=10s --timeout=10s --retries=10 CMD psql -c 'select 1' -d idam -U ${DB_USERNAME}

EXPOSE 5432