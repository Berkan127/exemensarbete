#!/bin/bash

# Restore script for PostgreSQL databases
# Usage: ./restore.sh <database_name> <backup_file>

set -e

# Configuration
DB_HOST="localhost"
DB_PORT="5432"
DB_USER="${DB_USERNAME:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-postgres}"

# Check arguments
if [ $# -ne 2 ]; then
    echo "Usage: $0 <database_name> <backup_file>"
    echo "Example: $0 userdb /backups/userdb_backup_20231207_120000.sql.gz"
    exit 1
fi

DB_NAME=$1
BACKUP_FILE=$2

# Check if backup file exists
if [ ! -f "$BACKUP_FILE" ]; then
    echo "ERROR: Backup file not found: $BACKUP_FILE"
    exit 1
fi

echo "Starting restore process at $(date)"
echo "Database: $DB_NAME"
echo "Backup file: $BACKUP_FILE"

# Create restore directory
RESTORE_DIR="/tmp/restore_$(date +%s)"
mkdir -p $RESTORE_DIR

# Function to restore database
restore_database() {
    local db_name=$1
    local backup_file=$2
    
    echo "Restoring database: $db_name"
    
    # Check if database exists, if so, drop it
    PGPASSWORD=$DB_PASSWORD psql \
        -h $DB_HOST \
        -p $DB_PORT \
        -U $DB_USER \
        -d postgres \
        -c "SELECT 1 FROM pg_database WHERE datname='$db_name'" | grep -q 1
    
    if [ $? -eq 0 ]; then
        echo "Database $db_name exists, dropping it..."
        PGPASSWORD=$DB_PASSWORD dropdb \
            -h $DB_HOST \
            -p $DB_PORT \
            -U $DB_USER \
            $db_name
    fi
    
    # Create database
    echo "Creating database: $db_name"
    PGPASSWORD=$DB_PASSWORD createdb \
        -h $DB_HOST \
        -p $DB_PORT \
        -U $DB_USER \
        $db_name
    
    # Extract backup if compressed
    if [[ $backup_file == *.gz ]]; then
        echo "Extracting compressed backup..."
        gunzip -c $backup_file > $RESTORE_DIR/restore.sql
        RESTORE_FILE="$RESTORE_DIR/restore.sql"
    else
        RESTORE_FILE=$backup_file
    fi
    
    # Restore database
    echo "Restoring from backup..."
    PGPASSWORD=$DB_PASSWORD pg_restore \
        -h $DB_HOST \
        -p $DB_PORT \
        -U $DB_USER \
        -d $db_name \
        --verbose \
        --clean \
        --if-exists \
        --no-owner \
        --no-privileges \
        $RESTORE_FILE
    
    if [ $? -eq 0 ]; then
        echo "Restore completed successfully for database: $db_name"
    else
        echo "ERROR: Restore failed for database: $db_name"
        exit 1
    fi
}

# Verify backup before restore
echo "Verifying backup file..."
if [[ $BACKUP_FILE == *.gz ]]; then
    if gzip -t $BACKUP_FILE; then
        echo "Backup file verification passed"
    else
        echo "ERROR: Backup file is corrupted"
        exit 1
    fi
fi

# Create restore point
echo "Creating restore point..."
PGPASSWORD=$DB_PASSWORD psql \
    -h $DB_HOST \
    -p $DB_PORT \
    -U $DB_USER \
    -d postgres \
    -c "CREATE TABLE IF NOT EXISTS restore_log (id SERIAL PRIMARY KEY, restore_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, database_name VARCHAR(50), backup_file VARCHAR(255), status VARCHAR(20));"

# Perform restore
restore_database $DB_NAME $BACKUP_FILE

# Log restore
PGPASSWORD=$DB_PASSWORD psql \
    -h $DB_HOST \
    -p $DB_PORT \
    -U $DB_USER \
    -d postgres \
    -c "INSERT INTO restore_log (database_name, backup_file, status) VALUES ('$DB_NAME', '$BACKUP_FILE', 'SUCCESS');"

# Clean up
rm -rf $RESTORE_DIR

echo "Restore process completed successfully at $(date)"

# Send notification (optional)
# curl -X POST -H 'Content-type: application/json' \
#   --data "{\"text\":\"Database restore completed for $DB_NAME from $BACKUP_FILE\"}" \
#   YOUR_SLACK_WEBHOOK_URL
