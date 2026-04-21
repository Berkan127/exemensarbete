# Security Implementation Guide

## Overview
This document describes the comprehensive security implementation for the microservices architecture, including authentication, authorization, and API security.

## Security Components

### 1. Authentication System

#### JWT (JSON Web Tokens)
- **Library**: JJWT 0.12.3
- **Algorithm**: HS256 (HMAC-SHA256)
- **Expiration**: 24 hours (configurable)
- **Secret**: Configurable via `jwt.secret` property

#### Token Structure
```json
{
  "sub": "username",
  "roles": ["ROLE_USER"],
  "iat": 1640995200,
  "exp": 1641081600
}
```

#### Authentication Flow
1. User registers via `/auth/register`
2. User logs in via `/auth/login` with credentials
3. System validates credentials and returns JWT
4. Client includes JWT in `Authorization: Bearer <token>` header
5. Gateway validates JWT and forwards request with user context

### 2. Authorization System

#### Role-Based Access Control (RBAC)
- **USER**: Basic access to own resources
- **MANAGER**: Can manage team resources
- **ADMIN**: Full system access

#### Permission Matrix
| Resource | USER | MANAGER | ADMIN |
|----------|------|---------|-------|
| Own Profile | READ/WRITE | READ/WRITE | READ/WRITE |
| Other Profiles | READ | READ/WRITE | READ/WRITE |
| Products | READ | READ/WRITE | READ/WRITE/DELETE |
| Orders | OWN | TEAM | ALL |
| Users | SELF | TEAM | ALL |

### 3. API Gateway Security

#### Security Filters Chain
1. **RateLimitingFilter**: 100 requests/minute per IP
2. **JwtAuthenticationFilter**: Validates JWT tokens
3. **ApiKeyAuthenticationFilter**: Validates API keys

#### Rate Limiting
- **Limit**: 100 requests per minute
- **Window**: 60 seconds sliding window
- **Headers**: `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`
- **Response**: HTTP 429 when limit exceeded

#### API Key Authentication
- **Header**: `X-API-Key`
- **Query Parameter**: `api_key`
- **Valid Keys**: 
  - `api-key-123456789` (Development)
  - `api-key-987654321` (Testing)
  - `api-key-admin-2024` (Production)

### 4. CORS Configuration

#### Allowed Origins
- `http://localhost:3000` (Development frontend)
- `http://localhost:3001` (Testing frontend)

#### Allowed Methods
- GET, POST, PUT, DELETE, OPTIONS

#### Allowed Headers
- All headers allowed
- Credentials supported

## Security Endpoints

### Authentication Endpoints
```
POST /api/auth/register    - User registration
POST /api/auth/login       - User login
POST /api/auth/refresh     - Token refresh
POST /api/auth/validate    - Token validation
```

### Protected Endpoints
```
GET    /api/products/*     - Requires authentication
POST   /api/products/*     - Requires authentication + API key
PUT    /api/products/*     - Requires authentication + API key
DELETE /api/products/*     - Requires ADMIN role + API key

GET    /api/orders/*       - Requires authentication
POST   /api/orders/*       - Requires authentication + API key
PUT    /api/orders/*       - Requires authentication + API key

GET    /api/users/*        - Requires authentication
PUT    /api/users/*        - Requires authentication + own profile
DELETE /api/users/*        - Requires ADMIN role
```

## Implementation Details

### User Service Security

#### Dependencies
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
```

#### Security Configuration
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfiguration {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

#### User Entity
```java
@Entity
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String username;
    
    @Column(nullable = false)
    private String password;
    
    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;
    
    // UserDetails implementation methods
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
```

### API Gateway Security

#### JWT Filter
```java
@Component
public class JwtAuthenticationFilter implements GatewayFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
        
        String token = authHeader.substring(7);
        if (!jwtService.validateToken(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
        
        // Add user context to downstream services
        ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-User-Name", jwtService.extractUsername(token))
                .header("X-User-Role", jwtService.extractRole(token))
                .build();
                
        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }
}
```

#### Rate Limiting Filter
```java
@Component
public class RateLimitingFilter implements GatewayFilter {
    private static final int RATE_LIMIT = 100; // requests per minute
    private static final long RATE_LIMIT_WINDOW_MS = 60_000; // 1 minute
    
    private final ConcurrentHashMap<String, RateLimitInfo> rateLimitMap = new ConcurrentHashMap<>();
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientIp = getClientIp(exchange.getRequest());
        
        if (isRateLimited(clientIp)) {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }
        
        return chain.filter(exchange);
    }
}
```

## Security Headers

### Gateway Response Headers
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1640995200
X-User-Name: john.doe
X-User-Role: USER
```

### CORS Headers
```
Access-Control-Allow-Origin: http://localhost:3000
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
Access-Control-Allow-Headers: *
Access-Control-Allow-Credentials: true
```

## Configuration

### Application Properties
```yaml
jwt:
  secret: mySecretKey123456789012345678901234567890
  expiration: 86400000 # 24 hours

spring:
  security:
    user:
      name: admin
      password: admin
```

### Gateway Security Configuration
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service-auth
          uri: http://localhost:8081
          predicates:
            - Path=/api/auth/**
          filters:
            - StripPrefix=1
            - RateLimitingFilter
            
        - id: product-service
          uri: http://localhost:8082
          predicates:
            - Path=/api/products/**
          filters:
            - StripPrefix=1
            - JwtAuthenticationFilter
            - RateLimitingFilter
            - ApiKeyAuthenticationFilter
```

## Security Testing

### Unit Tests
- **JWT Service Tests**: Token generation, validation, extraction
- **Authentication Tests**: Login, registration, token refresh
- **Authorization Tests**: Role-based access control
- **Filter Tests**: Rate limiting, API key validation

### Integration Tests
- **End-to-End Authentication**: Complete auth flow
- **Gateway Security**: Filter chain execution
- **CORS Testing**: Cross-origin requests
- **Rate Limiting**: Request throttling

### Security Tests
```java
@Test
void shouldAuthenticateSuccessfully() throws Exception {
    AuthRequest request = new AuthRequest("user", "password");
    
    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.username").value("user"));
}

@Test
void shouldRejectInvalidToken() throws Exception {
    mockMvc.perform(get("/api/products")
            .header("Authorization", "Bearer invalid.token"))
            .andExpect(status().isUnauthorized());
}

@Test
void shouldEnforceRateLimit() throws Exception {
    // Make 101 requests quickly
    for (int i = 0; i < 101; i++) {
        mockMvc.perform(get("/api/products"))
                .andExpect(i < 100 ? status().isOk() : status().isTooManyRequests());
    }
}
```

## Monitoring and Logging

### Security Events
- **Authentication Success/Failure**: Logged with user context
- **Authorization Failures**: Logged with attempted resource
- **Rate Limit Exceeded**: Logged with IP and timestamp
- **API Key Validation**: Logged with key identifier

### Metrics
- **Authentication Attempts**: Counter by success/failure
- **Rate Limit Hits**: Counter by IP address
- **Token Validations**: Counter by valid/invalid
- **Security Filter Latency**: Response time metrics

## Best Practices

### Password Security
- **Hashing**: BCrypt with strength 10
- **Validation**: Minimum 8 characters, complexity requirements
- **Storage**: Never store plain text passwords

### Token Security
- **Secret**: Use environment variables, not hardcoded
- **Expiration**: Reasonable token lifetime
- **Refresh**: Implement token refresh mechanism
- **Revocation**: Token blacklist for compromised tokens

### API Security
- **HTTPS**: Enforce in production
- **Input Validation**: Validate all inputs
- **SQL Injection**: Use parameterized queries
- **XSS Protection**: Input sanitization and output encoding

### Network Security
- **Firewall**: Restrict access to sensitive ports
- **VPN**: Use for administrative access
- **Network Segmentation**: Separate services by security level
- **DDoS Protection**: Rate limiting and request throttling

## Troubleshooting

### Common Issues

#### JWT Token Not Working
1. Check token format: `Bearer <token>`
2. Verify token is not expired
3. Check JWT secret configuration
4. Validate token signature

#### Rate Limiting Too Strict
1. Check IP detection logic
2. Verify rate limit configuration
3. Check for shared IP addresses
4. Adjust rate limit values

#### CORS Issues
1. Verify allowed origins configuration
2. Check preflight request handling
3. Validate credentials setting
4. Check browser console for errors

#### Authentication Failures
1. Verify user credentials
2. Check password encoding
3. Validate user exists and is enabled
4. Check authentication configuration

### Debug Logging
```yaml
logging:
  level:
    com.example.userservice.security: DEBUG
    com.example.apigateway.security: DEBUG
    org.springframework.security: DEBUG
```

## Future Enhancements

### Planned Improvements
1. **OAuth2 Authorization Server**: Full OAuth2/OIDC implementation
2. **Multi-Factor Authentication**: TOTP/SMS support
3. **API Key Management**: Dynamic key generation and rotation
4. **Advanced Rate Limiting**: User-based and tier-based limits
5. **Security Audit Logging**: Comprehensive audit trail
6. **Threat Detection**: Anomaly detection and alerting

### Additional Security Features
- **Web Application Firewall (WAF)** integration
- **Bot Detection and Mitigation**
- **IP Whitelisting/Blacklisting**
- **Request/Response Encryption**
- **Zero Trust Architecture** principles

## Conclusion

The security implementation provides comprehensive protection for the microservices architecture:

- **Strong Authentication**: JWT-based with secure token handling
- **Fine-Grained Authorization**: Role-based access control
- **API Gateway Security**: Multi-layered protection
- **Rate Limiting**: Protection against abuse
- **CORS Security**: Controlled cross-origin access
- **Comprehensive Testing**: Security validation at all levels

This security foundation ensures that the microservices are protected against common threats while maintaining usability and performance.
