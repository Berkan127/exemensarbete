# Monitoring & Logging Setup

## Overview
This document describes the complete monitoring and logging infrastructure implemented for the microservices architecture.

## Components

### 1. Prometheus - Metrics Collection
- **Port**: 9090
- **Purpose**: Collects metrics from all microservices
- **Configuration**: `monitoring/prometheus/prometheus.yml`
- **Scrape Intervals**: 
  - Services: 10s
  - Databases: 30s

### 2. Grafana - Visualization
- **Port**: 3000
- **Credentials**: admin/admin
- **Purpose**: Dashboards and alerts visualization
- **Data Sources**: Prometheus, Elasticsearch
- **Configuration**: `monitoring/grafana/provisioning/`

### 3. ELK Stack - Centralized Logging
- **Elasticsearch**: Port 9200 - Log storage and search
- **Logstash**: Port 5044 - Log processing pipeline
- **Kibana**: Port 5601 - Log visualization
- **Configuration**: `monitoring/logstash/`

### 4. Jaeger - Distributed Tracing
- **Port**: 16686 (UI)
- **Port**: 14268 (Collector)
- **Purpose**: Request tracing across services
- **Protocol**: OpenTelemetry

### 5. AlertManager - Alerting
- **Port**: 9093
- **Purpose**: Route alerts from Prometheus
- **Configuration**: `monitoring/alertmanager/alertmanager.yml`

## Service Configuration

### Logback Configuration
Each service includes:
- **JSON structured logging** with service metadata
- **File rotation** with 10MB max size, 30 days retention
- **Error separation** into dedicated error files
- **Profile-based** configuration (test vs production)

### Prometheus Metrics
Each service exposes:
- **JVM metrics**: Memory, CPU, GC
- **HTTP metrics**: Request count, duration, status codes
- **Database metrics**: Connection pool, query performance
- **Custom metrics**: Business-specific counters

### Health Checks
Custom health indicators:
- **Database connectivity**: PostgreSQL connection validation
- **Memory usage**: Alert if >90% used
- **External services**: Check other microservice availability

## Alerting Rules

### Critical Alerts
- **Service Down**: `up == 0` for 1 minute
- **Disk Space Low**: `<10%` available

### Warning Alerts
- **High Error Rate**: `>10%` 5xx responses for 2 minutes
- **High Response Time**: 95th percentile `>500ms` for 5 minutes
- **Database Connections**: `>80%` active connections
- **Memory Usage**: `>80%` heap usage
- **CPU Usage**: `>80%` for 5 minutes

## Access URLs

| Service | URL | Credentials |
|---------|-----|-------------|
| Prometheus | http://localhost:9090 | - |
| Grafana | http://localhost:3000 | admin/admin |
| Kibana | http://localhost:5601 | - |
| Jaeger | http://localhost:16686 | - |
| AlertManager | http://localhost:9093 | - |

## Docker Commands

### Start Monitoring Stack
```bash
docker-compose -f docker-compose-monitoring.yml up -d
```

### Stop Monitoring Stack
```bash
docker-compose -f docker-compose-monitoring.yml down
```

### View Logs
```bash
docker-compose -f docker-compose-monitoring.yml logs -f [service]
```

## Service Endpoints

### Product Service
- **Health**: http://localhost:8082/actuator/health
- **Metrics**: http://localhost:8082/actuator/prometheus
- **Info**: http://localhost:8082/actuator/info

### Order Service
- **Health**: http://localhost:8083/actuator/health
- **Metrics**: http://localhost:8083/actuator/prometheus

### User Service
- **Health**: http://localhost:8081/actuator/health
- **Metrics**: http://localhost:8081/actuator/prometheus

### API Gateway
- **Health**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8080/actuator/prometheus
- **Routes**: http://localhost:8080/actuator/gateway/routes

## Log Patterns

### Structured JSON Logs
```json
{
  "@timestamp": "2026-04-07T14:13:51.085+00:00",
  "level": "INFO",
  "service_name": "product-service",
  "service_version": "1.0.0",
  "message": "Request received",
  "logger": "com.example.productservice.controller.ProductController",
  "thread": "http-nio-8082-exec-1"
}
```

### Error Logs
```json
{
  "@timestamp": "2026-04-07T14:13:51.085+00:00",
  "level": "ERROR",
  "service_name": "product-service",
  "message": "Database connection failed",
  "exception": "org.postgresql.util.PSQLException",
  "stack_trace": "..."
}
```

## Performance Monitoring

### Key Metrics to Monitor
1. **Request Rate**: `http_requests_total`
2. **Response Time**: `http_request_duration_seconds`
3. **Error Rate**: `http_requests_total{status=~"5.."}`
4. **Memory Usage**: `jvm_memory_used_bytes`
5. **Database Connections**: `hikaricp_connections_active`

### SLA Targets
- **Response Time**: 95th percentile < 500ms
- **Error Rate**: < 1%
- **Availability**: > 99.9%
- **Memory Usage**: < 80%

## Troubleshooting

### Common Issues

#### Prometheus Not Scraping Metrics
1. Check service health: `curl http://localhost:8082/actuator/health`
2. Verify metrics endpoint: `curl http://localhost:8082/actuator/prometheus`
3. Check Prometheus targets: http://localhost:9090/targets

#### Grafana Not Connecting to Prometheus
1. Verify Prometheus is running: `docker ps | grep prometheus`
2. Check data source configuration in Grafana
3. Test connection: `curl http://localhost:9090/api/v1/query?query=up`

#### Elasticsearch Not Receiving Logs
1. Check Logstash logs: `docker logs logstash`
2. Verify Elasticsearch health: `curl http://localhost:9200/_cluster/health`
3. Check Logstash pipeline: `docker exec logstash logstash --config.test_and_exit`

#### Jaeger Not Showing Traces
1. Verify OpenTelemetry configuration in services
2. Check Jaeger collector: `curl http://localhost:14268/api/traces`
3. Verify service spans are being created

### Log Locations
- **Service Logs**: `logs/{service-name}.log`
- **Error Logs**: `logs/{service-name}-error.log`
- **Docker Logs**: `docker logs [container-name]`

## Security Considerations

### Production Recommendations
1. **Authentication**: Enable basic auth for Grafana
2. **HTTPS**: Configure SSL for all endpoints
3. **Network**: Restrict access to monitoring ports
4. **Data Retention**: Configure appropriate retention policies
5. **Backup**: Regular backups of Elasticsearch data

### Monitoring Security
- **Prometheus**: Enable authentication and authorization
- **Grafana**: Configure role-based access control
- **Kibana**: Set up security indices and permissions
- **AlertManager**: Secure webhook endpoints

## Scaling Considerations

### Horizontal Scaling
- **Prometheus**: Use federation for multiple instances
- **Elasticsearch**: Configure cluster mode
- **Grafana**: Load balance multiple instances

### Performance Optimization
- **Prometheus**: Adjust scrape intervals and retention
- **Elasticsearch**: Optimize index patterns and sharding
- **Logstash**: Configure appropriate workers and batch sizes

## Future Enhancements

### Planned Improvements
1. **Service Mesh**: Implement Istio for advanced observability
2. **APM**: Add Application Performance Monitoring
3. **Chaos Engineering**: Implement failure injection testing
4. **Machine Learning**: Anomaly detection in metrics
5. **Automated Remediation**: Self-healing capabilities

### Additional Tools
- **Zipkin**: Alternative distributed tracing
- **Fluentd**: Alternative log collector
- **VictoriaMetrics**: Prometheus alternative
- **Thanos**: Long-term Prometheus storage

## Conclusion

The monitoring and logging infrastructure provides comprehensive observability for the microservices architecture. It enables:

- **Proactive monitoring** through alerts and dashboards
- **Root cause analysis** with distributed tracing
- **Performance optimization** through metrics analysis
- **Compliance auditing** through centralized logging
- **Operational excellence** through automated alerting

This setup ensures that the microservices can be effectively monitored, troubleshot, and optimized in both development and production environments.
