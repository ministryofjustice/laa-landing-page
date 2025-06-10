#!/bin/bash

# A utility script for interacting with the laa-shared-database docker container.

# --- Configuration ---
# The name of your postgres docker-compose service.
DOCKER_SERVICE_NAME="laa-shared-database"

# --- Helper Functions ---

# Function to display help information
show_help() {
  echo "Database Interaction Tool for '$DOCKER_SERVICE_NAME'"
  echo "----------------------------------------------------"
  echo "Usage: ./db-tool.sh [command] [options]"
  echo
  echo "Available Commands:"
  echo "  help                            Shows this help message."
  echo
  echo "  get table <table_name>          Fetches all rows from the specified table."
  echo "                                  Example: ./db-tool.sh get table users"
  echo
  echo "  get schema <table_name>         Shows the schema (column definitions) for a table."
  echo "                                  Example: ./db-tool.sh get schema users"
  echo
  echo "  list tables                     Lists all tables in the public schema."
  echo
  echo "  list schemas                    Lists all schemas in the database."
  echo
  echo "  list users                      Lists all users (roles) in the database."
  echo
  echo "  psql                            Opens an interactive psql shell to the database."
  echo
  echo "  clear database                  Deletes all rows from all tables in the public schema."
  echo "                                  (This is a destructive action and will ask for confirmation)."
  echo
}

# --- Pre-flight Checks ---

# 1. Check for required environment variables
if [ -z "$POSTGRES_DB_NAME" ] || [ -z "$POSTGRES_USERNAME" ] || [ -z "$POSTGRES_PASSWORD" ] || [ -z "$POSTGRES_DB_ADDRESS" ]; then
  echo "Error: Database information not set as environment variables." >&2
  echo "Please ensure the .env file has been exported or your environment variables are otherwise set." >&2
  exit 1
fi

# 2. Check if docker-compose service is running
if ! docker-compose ps -q "$DOCKER_SERVICE_NAME" &>/dev/null || [ -z "$(docker-compose ps -q "$DOCKER_SERVICE_NAME")" ]; then
    echo "Error: The docker-compose service '$DOCKER_SERVICE_NAME' is not running." >&2
    echo "Please start your services with 'docker-compose up -d' first." >&2
    exit 1
fi


# --- Command Router ---

# Show help if no arguments are provided
if [ $# -eq 0 ]; then
  show_help
  exit 0
fi

# Set the command and shift arguments
COMMAND=$1
shift

case "$COMMAND" in
  help|-h|--help)
    show_help
    ;;

  get)
    SUBCMD=$1
    if [ "$SUBCMD" == "table" ]; then
      if [ -z "$2" ]; then
        echo "Error: Please provide a table name." >&2
        echo "Usage: ./db-tool.sh get table <table_name>" >&2
        exit 1
      fi
      TABLE_NAME=$2
      echo "Fetching all rows from table: $TABLE_NAME..."
      docker-compose exec -T -e PGPASSWORD="$POSTGRES_PASSWORD" "$DOCKER_SERVICE_NAME" \
        psql -U "$POSTGRES_USERNAME" -d "$POSTGRES_DB_NAME" -c "SELECT * FROM $TABLE_NAME;"
    elif [ "$SUBCMD" == "schema" ]; then
      if [ -z "$2" ]; then
        echo "Error: Please provide a table name." >&2
        echo "Usage: ./db-tool.sh get schema <table_name>" >&2
        exit 1
      fi
      TABLE_NAME=$2
      echo "Fetching schema for table: $TABLE_NAME..."
      docker-compose exec -T -e PGPASSWORD="$POSTGRES_PASSWORD" "$DOCKER_SERVICE_NAME" \
        psql -U "$POSTGRES_USERNAME" -d "$POSTGRES_DB_NAME" -c "\d $TABLE_NAME"
    else
      echo "Error: Unknown 'get' command. Use 'table' or 'schema'." >&2
      show_help
      exit 1
    fi
    ;;

  list)
    SUBCMD=$1
    if [ "$SUBCMD" == "tables" ]; then
        echo "Listing all tables..."
        docker-compose exec -T -e PGPASSWORD="$POSTGRES_PASSWORD" "$DOCKER_SERVICE_NAME" \
          psql -U "$POSTGRES_USERNAME" -d "$POSTGRES_DB_NAME" -c "\dt"
    elif [ "$SUBCMD" == "schemas" ]; then
        echo "Listing all schemas..."
        docker-compose exec -T -e PGPASSWORD="$POSTGRES_PASSWORD" "$DOCKER_SERVICE_NAME" \
          psql -U "$POSTGRES_USERNAME" -d "$POSTGRES_DB_NAME" -c "\dn"
    elif [ "$SUBCMD" == "users" ]; then
        echo "Listing all users/roles..."
        docker-compose exec -T -e PGPASSWORD="$POSTGRES_PASSWORD" "$DOCKER_SERVICE_NAME" \
          psql -U "$POSTGRES_USERNAME" -d "$POSTGRES_DB_NAME" -c "\du"
    else
        echo "Error: Unknown 'list' command. Use 'tables', 'schemas', or 'users'." >&2
        show_help
        exit 1
    fi
    ;;

  clear)
    if [ "$1" == "database" ]; then
      echo "WARNING: This will delete all data from all tables in the public schema of '$POSTGRES_DB_NAME'."
      read -p "Are you sure you want to continue? [y/N] " -n 1 -r
      echo # move to a new line
      if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "Clearing database..."
        # This command generates and executes a TRUNCATE statement for each table.
        # TRUNCATE is faster than DELETE and also resets auto-incrementing sequences.
        # CASCADE drops foreign-key references from other tables.
        docker-compose exec -T -e PGPASSWORD="$POSTGRES_PASSWORD" "$DOCKER_SERVICE_NAME" \
          psql -U "$POSTGRES_USERNAME" -d "$POSTGRES_DB_NAME" -c "DO \$\$ DECLARE r RECORD; BEGIN FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = 'public') LOOP EXECUTE 'TRUNCATE TABLE ' || quote_ident(r.tablename) || ' RESTART IDENTITY CASCADE'; END LOOP; END \$\$;"
        echo "Database cleared."
      else
        echo "Operation cancelled."
      fi
    else
        echo "Error: Unknown 'clear' command. Did you mean 'clear database'?" >&2
        exit 1
    fi
    ;;

  psql)
    echo "Connecting to database '$POSTGRES_DB_NAME' with user '$POSTGRES_USERNAME'..."
    # Note: We use `docker-compose exec` without -T here for an interactive terminal
    docker-compose exec -e PGPASSWORD="$POSTGRES_PASSWORD" "$DOCKER_SERVICE_NAME" \
      psql -U "$POSTGRES_USERNAME" -d "$POSTGRES_DB_NAME"
    ;;

  *)
    echo "Error: Unknown command '$COMMAND'" >&2
    show_help
    exit 1
    ;;
esac
