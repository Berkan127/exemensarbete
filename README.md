# Exarbete Microservices - Del 1

Detta repository innehaller endast Del 1 enligt uppgiften: projektstruktur och arkitektur for ett mikrotjanstbaserat system.

## Struktur

- `user-service` - separat Spring Boot-projekt
- `product-service` - separat Spring Boot-projekt
- `order-service` - separat Spring Boot-projekt
- `api-gateway` - separat Spring Boot-projekt
- `docker-compose.yml` - grundstruktur for lokal containerorkestrering

## Del 1 omfattning

Foljande ar gjort:

1. Grundstruktur for hela mikrotjanstsystemet
2. Separata projekt for User, Product, Order och API Gateway
3. Maven-konfiguration med grundlaggande beroenden
4. Docker-struktur med separata databaser per tjanst

Foljande ar medvetet inte gjort i Del 1:

- Datamodeller och endpoints
- Inter-service kommunikation
- Affarslogik for orders
- Gateway-routingregler
- Avancerad felhantering, logging, tester och deployment i AWS
