#!/bin/bash

# Backup script for PostgreSQL databases
# Usage: ./backup.sh

set -e

# Configuration
DB_HOST="localhost"
DB_PORT="5432"
DB_USER="${DB_USERNAME:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-postgres}"
BACKUP_DIR="/backups"
DATE=$(date +%Y%m%d_%H%M%S)
RETENTION_DAYS=30

# Databases to backup
DATABASES=("userdb" "productdb" "orderdb")

# Create backup directory
mkdir -p $BACKUP_DIR

echo "Starting backup process at $(date)"

# Function to backup a single database
backup_database() {
    local db_name=$1
    local backup_file="$BACKUP_DIR/${db_name}_backup_$DATE.sql"
    
    echo "Backing up database: $db_name"
    
    PGPASSWORD=$DB_PASSWORD pg_dump \
        -h $DB_HOST \
        -p $DB_PORT \
        -U $DB_USER \
        -d $db_name \
        --verbose \
        --clean \
        --if-exists \
        --create \
        --format=custom \
        --compress=9 \
        --file=$backup_file
    
    if [ $? -eq 0 ]; then
        echo "Backup completed: $backup_file"
        
        # Compress the backup
        gzip $backup_file
        echo "Compressed: ${backup_file}.gz"
        
        # Verify backup integrity
        if gzip -t ${backup_file}.gz; then
            echo "Backup verified: ${backup_file}.gz"
        else
            echo "ERROR: Backup verification failed for ${backup_file}.gz"
            exit 1
        fi
    else
        echo "ERROR: Backup failed for database: $db_name"
        exit 1
    fi
}

# Backup each database
for db in "${DATABASES[@]}"; do
    backup_database $db
done

# Clean up old backups
echo "Cleaning up backups older than $RETENTION_DAYS days"
find $BACKUP_DIR -name "*.gz" -mtime +$RETENTION_DAYS -delete

# Create backup manifest
cat > $BACKUP_DIR/backup_manifest_$DATE.txt << EOF
Backup completed at: $(date)
Databases backed up: ${DATABASES[*]}
Backup files:
$(ls -la $BACKUP_DIR/*_$DATE.sql.gz 2>/dev/null || echo "No backup files found")
EOF

echo "Backup process completed successfully at $(date)"
echo "Manifest: $BACKUP_DIR/backup_manifest_$DATE.txt"

# Send notification (optional)
# curl -X POST -H 'Content-type: application/json' \
#   --data '{"text":"Database backup completed successfully"}' \
#   YOUR_SLACK_WEBHOOK_URL
