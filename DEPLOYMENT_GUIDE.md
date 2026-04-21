# Deployment Guide

## Overview
This document provides comprehensive instructions for deploying the microservices architecture to production environments using both Docker Compose and Kubernetes.

## Deployment Options

### 1. Docker Compose (Development/Small Production)
- **Use Case**: Development, testing, small production environments
- **Complexity**: Low
- **Scalability**: Limited
- **Management**: Manual

### 2. Kubernetes (Production/Enterprise)
- **Use Case**: Production, enterprise environments
- **Complexity**: High
- **Scalability**: Excellent
- **Management**: Automated

## Prerequisites

### System Requirements
- **CPU**: 4+ cores
- **Memory**: 8GB+ RAM
- **Storage**: 50GB+ available space
- **Network**: Stable internet connection

### Software Requirements
- **Docker**: 20.10+
- **Docker Compose**: 2.0+
- **Kubernetes**: 1.28+ (for K8s deployment)
- **kubectl**: 1.28+ (for K8s deployment)
- **Git**: 2.30+

## Docker Compose Deployment

### 1. Environment Setup

Create environment file:
```bash
cp .env.prod.example .env.prod
```

Edit `.env.prod`:
```bash
# Database Configuration
DB_USERNAME=postgres
DB_PASSWORD=your_secure_password_here

# Security
JWT_SECRET=mySecretKey123456789012345678901234567890

# Monitoring
GRAFANA_PASSWORD=admin123

# Performance
JAVA_OPTS=-Xms512m -Xmx1g -XX:+UseG1GC
SPRING_PROFILES_ACTIVE=production
```

### 2. Build and Deploy

```bash
# Build all services
docker-compose -f docker-compose.prod.yml build

# Deploy all services
docker-compose -f docker-compose.prod.yml up -d

# Verify deployment
docker-compose -f docker-compose.prod.yml ps
```

### 3. Health Checks

```bash
# Check service health
curl http://localhost:8080/actuator/health

# Check individual services
curl http://localhost:8081/actuator/health  # User Service
curl http://localhost:8082/actuator/health  # Product Service
curl http://localhost:8083/actuator/health  # Order Service
```

### 4. Monitoring Access

- **API Gateway**: http://localhost:8080
- **Grafana**: http://localhost:3000 (admin/admin123)
- **Prometheus**: http://localhost:9090

## Kubernetes Deployment

### 1. Cluster Setup

#### Option A: Local Development (Minikube)
```bash
# Install Minikube
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube

# Start cluster
minikube start --cpus=4 --memory=8g --disk-size=50g

# Enable addons
minikube addons enable ingress
minikube addons enable metrics-server
```

#### Option B: Cloud Provider
- **AWS EKS**: Use eksctl or AWS Console
- **Google GKE**: Use gcloud CLI or Console
- **Azure AKS**: Use az CLI or Console

### 2. Prepare Kubernetes Resources

#### Create Namespace
```bash
kubectl apply -f k8s/namespace.yaml
```

#### Create Secrets
```bash
# Update secrets with your values
kubectl apply -f k8s/secrets.yaml
```

#### Create ConfigMaps
```bash
kubectl apply -f k8s/configmap.yaml
```

### 3. Deploy Infrastructure

#### Deploy Databases and Cache
```bash
# Deploy PostgreSQL
kubectl apply -f k8s/postgres.yaml

# Deploy Redis
kubectl apply -f k8s/redis.yaml

# Wait for pods to be ready
kubectl wait --for=condition=ready pod -l app=postgres -n microservices --timeout=300s
kubectl wait --for=condition=ready pod -l app=redis -n microservices --timeout=300s
```

#### Deploy Applications
```bash
# Deploy User Service
kubectl apply -f k8s/user-service.yaml

# Deploy Product Service
kubectl apply -f k8s/product-service.yaml

# Deploy Order Service
kubectl apply -f k8s/order-service.yaml

# Deploy API Gateway
kubectl apply -f k8s/api-gateway.yaml

# Wait for all pods to be ready
kubectl wait --for=condition=ready pod -l tier=application -n microservices --timeout=600s
```

#### Deploy Ingress
```bash
kubectl apply -f k8s/ingress.yaml
```

### 4. Verify Deployment

```bash
# Check pod status
kubectl get pods -n microservices

# Check services
kubectl get services -n microservices

# Check ingress
kubectl get ingress -n microservices

# Check logs
kubectl logs -f deployment/api-gateway -n microservices
```

### 5. Access Services

```bash
# Get ingress URL
kubectl get ingress microservices-ingress -n microservices

# Test API
curl https://api.microservices.example.com/actuator/health
```

## CI/CD Pipeline

### GitHub Actions Configuration

The CI/CD pipeline is configured in `.github/workflows/ci-cd.yml`:

#### Pipeline Stages
1. **Test**: Unit and integration tests
2. **Security Scan**: Vulnerability scanning with Trivy
3. **Build**: Build and push Docker images
4. **Deploy Staging**: Deploy to staging environment
5. **Deploy Production**: Deploy to production environment
6. **Performance Test**: Load testing with k6
7. **Notify**: Slack notifications

#### Environment Variables
Set these in GitHub repository settings:
- `KUBE_CONFIG_STAGING`: Base64 encoded kubeconfig for staging
- `KUBE_CONFIG_PRODUCTION`: Base64 encoded kubeconfig for production
- `SLACK_WEBHOOK`: Slack webhook URL for notifications

#### Pipeline Triggers
- **Push to main**: Full deployment to production
- **Push to develop**: Deployment to staging
- **Pull Request**: Tests and security scan only

## Environment Configuration

### Development Environment
```yaml
spring:
  profiles:
    active: development
  datasource:
    url: jdbc:postgresql://localhost:55434/productdb
  redis:
    host: localhost
    port: 6379
```

### Staging Environment
```yaml
spring:
  profiles:
    active: staging
  datasource:
    url: jdbc:postgresql://postgres-staging:5432/productdb
  redis:
    host: redis-staging
    port: 6379
```

### Production Environment
```yaml
spring:
  profiles:
    active: production
  datasource:
    url: jdbc:postgresql://postgres-service:5432/productdb
  redis:
    host: redis-service
    port: 6379
```

## Secrets Management

### Kubernetes Secrets

#### Database Credentials
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: microservices-secrets
type: Opaque
data:
  db-username: cG9zdGdyZXM=
  db-password: cG9zdGdyZXM=
  jwt-secret: bXlTZWNyZXRLZXkxMjM0NTY3ODkwMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=
```

#### Managing Secrets
```bash
# Create secret from file
kubectl create secret generic db-credentials --from-file=username.txt --from-file=password.txt

# Update secret
kubectl patch secret microservices-secrets -p='{"data":{"jwt-secret":"bmV3X3NlY3JldA=="}}'

# Decode secret
kubectl get secret microservices-secrets -o jsonpath='{.data.jwt-secret}' | base64 -d
```

### Docker Compose Secrets
```bash
# Use .env file for secrets
echo "DB_PASSWORD=your_secure_password" >> .env

# Or use Docker secrets (Swarm mode)
echo "your_secure_password" | docker secret create db_password -
```

## Backup and Recovery

### Automated Backup

#### Database Backup Script
```bash
#!/bin/bash
# Daily backup at 2 AM
0 2 * * * /path/to/scripts/backup.sh

# Manual backup
./scripts/backup.sh
```

#### Backup Configuration
- **Frequency**: Daily at 2 AM
- **Retention**: 30 days
- **Compression**: gzip
- **Verification**: Integrity check after backup

### Disaster Recovery

#### Recovery Procedures
```bash
# Full recovery
./scripts/disaster-recovery.sh full

# Partial recovery
./scripts/disaster-recovery.sh partial

# Restore specific database
./scripts/restore.sh userdb /backups/userdb_backup_20231207_120000.sql.gz
```

#### Recovery Testing
```bash
# Test recovery monthly
./scripts/test-recovery.sh

# Verify backup integrity
./scripts/verify-backups.sh
```

## Monitoring and Observability

### Health Checks

#### Kubernetes Health Checks
```yaml
livenessProbe:
  httpGet:
    path: /actuator/health
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 30
  timeoutSeconds: 10
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3
```

#### Custom Health Indicators
- **Database Health**: Connection validation
- **Redis Health**: Cache connectivity
- **External Service Health**: Dependency checks

### Metrics and Logging

#### Prometheus Metrics
- **Application Metrics**: Custom business metrics
- **JVM Metrics**: Memory, GC, threads
- **HTTP Metrics**: Request count, duration, status
- **Database Metrics**: Connection pool, query time

#### Structured Logging
```json
{
  "@timestamp": "2023-12-07T12:00:00.000Z",
  "level": "INFO",
  "service_name": "product-service",
  "message": "Request processed",
  "trace_id": "abc123",
  "span_id": "def456"
}
```

## Scaling and Performance

### Horizontal Pod Autoscaling

#### HPA Configuration
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: product-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: product-service
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

#### Scaling Policies
- **Scale Up**: 50% increase, 60s stabilization
- **Scale Down**: 10% decrease, 300s stabilization
- **CPU Threshold**: 70% average utilization
- **Memory Threshold**: 80% average utilization

### Performance Optimization

#### Resource Limits
```yaml
resources:
  requests:
    memory: "512Mi"
    cpu: "500m"
  limits:
    memory: "1Gi"
    cpu: "1000m"
```

#### JVM Tuning
```bash
JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

## Security

### Network Security

#### Kubernetes Network Policies
```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: microservices-netpol
spec:
  podSelector:
    matchLabels:
      tier: application
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - podSelector:
        matchLabels:
          tier: application
    ports:
    - protocol: TCP
      port: 8080
```

#### Ingress Security
- **TLS Termination**: SSL/TLS encryption
- **Rate Limiting**: 100 requests/minute
- **Authentication**: JWT validation
- **CORS**: Cross-origin resource sharing

### Container Security

#### Security Scanning
```bash
# Scan images with Trivy
trivy image product-service:latest

# Scan with Clair
clairctl scan product-service:latest
```

#### Runtime Security
- **Non-root User**: Run containers as non-root
- **Read-only Filesystem**: Immutable containers
- **Resource Limits**: Prevent resource exhaustion
- **Seccomp**: System call filtering

## Troubleshooting

### Common Issues

#### Pod Not Starting
```bash
# Check pod events
kubectl describe pod <pod-name> -n microservices

# Check logs
kubectl logs <pod-name> -n microservices

# Check resource usage
kubectl top pods -n microservices
```

#### Database Connection Issues
```bash
# Test database connectivity
kubectl exec -it <postgres-pod> -n microservices -- psql -U postgres -d productdb

# Check database logs
kubectl logs deployment/postgres -n microservices
```

#### Service Discovery Issues
```bash
# Check service endpoints
kubectl get endpoints -n microservices

# Test service connectivity
kubectl exec -it <pod-name> -n microservices -- nslookup postgres-service
```

### Debug Commands

#### General Debugging
```bash
# Get cluster info
kubectl cluster-info

# Get node status
kubectl get nodes

# Get all resources
kubectl get all -n microservices

# Watch resources
kubectl get pods -n microservices -w
```

#### Application Debugging
```bash
# Port forward to local
kubectl port-forward service/product-service 8082:8082 -n microservices

# Exec into container
kubectl exec -it <pod-name> -n microservices -- /bin/bash

# Check environment variables
kubectl exec <pod-name> -n microservices -- env
```

## Maintenance

### Rolling Updates

#### Update Strategy
```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxUnavailable: 25%
    maxSurge: 25%
```

#### Update Commands
```bash
# Update image
kubectl set image deployment/product-service product-service=new-image:latest -n microservices

# Rollout status
kubectl rollout status deployment/product-service -n microservices

# Rollback
kubectl rollout undo deployment/product-service -n microservices
```

### Maintenance Windows

#### Scheduled Maintenance
```bash
# Scale down services
kubectl scale deployment product-service --replicas=0 -n microservices

# Perform maintenance
# ... maintenance tasks ...

# Scale up services
kubectl scale deployment product-service --replicas=3 -n microservices
```

## Best Practices

### Development
- **Environment Parity**: Keep dev/staging/prod similar
- **Infrastructure as Code**: Version all configurations
- **Automated Testing**: Comprehensive test coverage
- **Security First**: Security in all stages

### Operations
- **Monitoring**: Comprehensive observability
- **Backup Strategy**: Regular backups and testing
- **Documentation**: Up-to-date documentation
- **Automation**: Automate repetitive tasks

### Security
- **Principle of Least Privilege**: Minimal permissions
- **Regular Updates**: Keep dependencies updated
- **Security Scanning**: Regular vulnerability scans
- **Incident Response**: Have a response plan

## Conclusion

This deployment guide provides a comprehensive approach to deploying and managing the microservices architecture:

- **Multiple Deployment Options**: Docker Compose and Kubernetes
- **Comprehensive CI/CD**: Automated pipeline with testing and deployment
- **Production Ready**: Security, monitoring, and scaling
- **Disaster Recovery**: Backup and recovery procedures
- **Best Practices**: Industry-standard deployment practices

Following this guide ensures reliable, scalable, and maintainable deployments of the microservices architecture.
