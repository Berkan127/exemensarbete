# Mikrotjänstsystem

Det här repo:t innehåller ** Bas uppladningen ** av  grundstruktur och arkitektur för ett mikrotjänstbaserat system enligt specifikationen.

## Tjänster (separata kodbaser)

- `user-service` (port **8081**) – Java 17, Spring Boot, JPA, PostgreSQL
- `product-service` (port **8082**) – Java 17, Spring Boot, JPA, PostgreSQL
- `order-service` (port **8083**) – Java 17, Spring Boot, JPA, PostgreSQL
- `api-gateway` (port **8080**) – Spring Cloud Gateway (enda entry point)

## Databaser (ingen delad databas)

Tre separata PostgreSQL-instanser (en per tjänst):

- `users_db`
- `products_db`
- `orders_db`

## Lokal körning (Docker Compose)

Förutsättning: Docker Desktop är installerat och igång.

Starta allt:

```bash
docker compose up --build
```

Stoppa allt:

```bash
docker compose down
```

Gateway routar anrop till respektive tjänst:

- `/users/**` → User Service
- `/products/**` → Product Service
- `/orders/**` → Order Service

Health endpoints (exempel):

- `GET http://localhost:8080/actuator/health` (gateway)
- `GET http://localhost:8081/actuator/health` (user-service)
- `GET http://localhost:8082/actuator/health` (product-service)
- `GET http://localhost:8083/actuator/health` (order-service)

## Felsökning

- Första uppstarten kan ta tid eftersom Docker laddar ner images.
- Kontrollera att portar inte är upptagna: `8080-8083`, `5433-5435`.
- Om något hänger: `docker compose down` och sedan `docker compose up --build` igen.

## Nästa steg (Del 2)

Implementera User Service:

- datamodell
- egen PostgreSQL-koppling
- endpoints: `POST /users`, `GET /users/{id}`

