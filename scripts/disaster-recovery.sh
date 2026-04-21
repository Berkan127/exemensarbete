#!/bin/bash

# Disaster Recovery script
# Usage: ./disaster-recovery.sh [full|partial]

set -e

# Configuration
BACKUP_DIR="/backups"
RESTORE_LOG="/tmp/disaster_recovery_$(date +%Y%m%d_%H%M%S).log"
RECOVERY_TYPE=${1:-full}

# Function to log messages
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a $RESTORE_LOG
}

# Function to check system health
check_system_health() {
    log "Checking system health..."
    
    # Check Docker
    if ! docker --version > /dev/null 2>&1; then
        log "ERROR: Docker is not running"
        return 1
    fi
    
    # Check Docker Compose
    if ! docker-compose --version > /dev/null 2>&1; then
        log "ERROR: Docker Compose is not available"
        return 1
    fi
    
    # Check disk space
    DISK_USAGE=$(df / | awk 'NR==2 {print $5}' | sed 's/%//')
    if [ $DISK_USAGE -gt 90 ]; then
        log "WARNING: Disk usage is ${DISK_USAGE}%"
    fi
    
    log "System health check completed"
    return 0
}

# Function to stop services
stop_services() {
    log "Stopping all services..."
    
    # Stop Docker Compose services
    if [ -f "docker-compose.prod.yml" ]; then
        docker-compose -f docker-compose.prod.yml down || true
    fi
    
    # Wait for services to stop
    sleep 10
    
    log "Services stopped"
}

# Function to start services
start_services() {
    log "Starting services..."
    
    # Start Docker Compose services
    if [ -f "docker-compose.prod.yml" ]; then
        docker-compose -f docker-compose.prod.yml up -d || true
    fi
    
    # Wait for services to start
    sleep 30
    
    log "Services started"
}

# Function to verify services
verify_services() {
    log "Verifying services..."
    
    # Check database connectivity
    services=("user-db:5432" "product-db:5432" "order-db:5432" "redis:6379")
    
    for service in "${services[@]}"; do
        IFS=':' read -r host port <<< "$service"
        if nc -z $host $port; then
            log "Service $host:$port is reachable"
        else
            log "WARNING: Service $host:$port is not reachable"
        fi
    done
    
    # Check application health
    apps=("user-service:8081" "product-service:8082" "order-service:8083" "api-gateway:8080")
    
    for app in "${apps[@]}"; do
        IFS=':' read -r host port <<< "$app"
        if curl -f -s http://localhost:$port/actuator/health > /dev/null; then
            log "Application $app is healthy"
        else
            log "WARNING: Application $app is not healthy"
        fi
    done
    
    log "Service verification completed"
}

# Function to perform full recovery
full_recovery() {
    log "Starting full disaster recovery..."
    
    # Stop services
    stop_services
    
    # Restore databases
    databases=("userdb" "productdb" "orderdb")
    
    for db in "${databases[@]}"; do
        log "Restoring database: $db"
        
        # Find latest backup
        latest_backup=$(ls -t $BACKUP_DIR/${db}_backup_*.sql.gz 2>/dev/null | head -1)
        
        if [ -n "$latest_backup" ]; then
            log "Using latest backup: $latest_backup"
            ./scripts/restore.sh $db $latest_backup
        else
            log "WARNING: No backup found for database: $db"
        fi
    done
    
    # Start services
    start_services
    
    # Verify services
    verify_services
    
    log "Full disaster recovery completed"
}

# Function to perform partial recovery
partial_recovery() {
    log "Starting partial disaster recovery..."
    
    # Get user input for what to recover
    echo "Available recovery options:"
    echo "1. Restore specific database"
    echo "2. Restart services only"
    echo "3. Verify services only"
    
    read -p "Enter your choice (1-3): " choice
    
    case $choice in
        1)
            read -p "Enter database name (userdb/productdb/orderdb): " dbname
            read -p "Enter backup file path: " backup_file
            ./scripts/restore.sh $dbname $backup_file
            ;;
        2)
            stop_services
            start_services
            verify_services
            ;;
        3)
            verify_services
            ;;
        *)
            log "Invalid choice"
            exit 1
            ;;
    esac
    
    log "Partial disaster recovery completed"
}

# Main recovery process
main() {
    log "Starting disaster recovery process"
    log "Recovery type: $RECOVERY_TYPE"
    
    # Check system health
    if ! check_system_health; then
        log "ERROR: System health check failed"
        exit 1
    fi
    
    case $RECOVERY_TYPE in
        full)
            full_recovery
            ;;
        partial)
            partial_recovery
            ;;
        *)
            log "ERROR: Invalid recovery type. Use 'full' or 'partial'"
            exit 1
            ;;
    esac
    
    log "Disaster recovery process completed successfully"
    log "Recovery log: $RESTORE_LOG"
    
    # Send notification (optional)
    # curl -X POST -H 'Content-type: application/json' \
    #   --data "{\"text\":\"Disaster recovery completed successfully\"}" \
    #   YOUR_SLACK_WEBHOOK_URL
}

# Execute main function
main
