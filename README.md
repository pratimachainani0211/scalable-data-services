# Scalable Data Services

A production-ready multi-tenant data architecture built with Spring Boot, demonstrating enterprise-grade microservices patterns for data management with proper isolation, scaling, and monitoring.

## Architecture Overview

This project showcases a comprehensive multi-tenant data platform that handles different data types across multiple databases while maintaining strict tenant isolation and high performance.

### Core Architectural Concepts

#### 1. Multi-Tenant Architecture Pattern
**Design**: Shared application with data-level isolation
- **Tenant Identification**: HTTP header-based (`X-Tenant-ID`) for simplicity and flexibility
- **Data Isolation**: Database queries filtered by tenant ID at the repository level
- **Context Management**: Thread-local storage ensures tenant context throughout request lifecycle

**Rationale**: This approach provides cost-effective multi-tenancy while maintaining security boundaries. It's simpler than database-per-tenant but more secure than shared tables without isolation.

#### 2. Polyglot Persistence Strategy
**Design**: Multiple databases optimized for different data patterns
- **PostgreSQL**: Relational data (users) requiring ACID properties and complex queries
- **MongoDB**: Document storage (products) for flexible schema and rapid iteration
- **Redis**: Distributed caching for performance optimization

**Rationale**: Each database serves its optimal use case, providing better performance and developer experience than forcing all data into a single database type.

#### 3. Layered Service Architecture
**Design**: Clean separation of concerns across layers
```
Controllers → Services → Repositories → Data Sources
```
- **Controllers**: HTTP boundary, request/response handling
- **Services**: Business logic, caching, transaction management
- **Repositories**: Data access abstraction with tenant-aware queries
- **Models**: Domain entities with validation

**Rationale**: This pattern ensures maintainability, testability, and clear separation of responsibilities. Each layer has a single purpose and can be modified independently.

#### 4. Caching Strategy
**Design**: Multi-level caching with tenant isolation
- **Cache Keys**: Include tenant ID to prevent data leakage
- **TTL Strategy**: 10-minute default with configurable expiration
- **Cache-Aside Pattern**: Application manages cache population and invalidation

**Rationale**: Improves response times significantly while maintaining tenant data isolation. Redis provides distributed caching for horizontal scaling.

#### 5. Configuration Management
**Design**: Environment-specific configurations with sensible defaults
- **Profile-based**: Separate configs for dev, staging, production
- **External Configuration**: Environment variables for sensitive data
- **Default Values**: Development-friendly defaults with production overrides

**Rationale**: Enables smooth deployment across environments while keeping secrets secure and configurations maintainable.

#### 6. Monitoring and Observability
**Design**: Built-in metrics and health monitoring
- **Prometheus Metrics**: Application and JVM metrics exposed
- **Health Checks**: Database connectivity and application status
- **Structured Logging**: Tenant ID included in log context

**Rationale**: Essential for production operations, enabling proactive monitoring and quick troubleshooting in multi-tenant environments.

## Technology Stack

| Component | Technology | Purpose |
|-----------|------------|---------|
| **Framework** | Spring Boot 3.2 | Application foundation with auto-configuration |
| **Build Tool** | Gradle | Dependency management and build automation |
| **Databases** | PostgreSQL + MongoDB | Polyglot persistence for optimal data handling |
| **Caching** | Redis | Distributed caching for performance |
| **Monitoring** | Prometheus + Actuator | Metrics collection and health monitoring |
| **Containerization** | Docker + Docker Compose | Development and deployment environment |
| **CI/CD** | GitHub Actions | Automated testing and deployment |

## Getting Started

### Prerequisites

- **Java 17+** (OpenJDK recommended)
- **Docker & Docker Compose** (for local infrastructure)
- **Git** (for cloning the repository)

### Local Development Setup

#### 1. Clone the Repository
```bash
git clone https://github.com/yourusername/scalable-data-services.git
cd scalable-data-services
```

#### 2. Start Infrastructure Services
```bash
# Start PostgreSQL, MongoDB, Redis, and Prometheus
docker-compose up -d postgres mongodb redis prometheus

# Verify services are running
docker-compose ps

# Clear Redis cache (fixes serialization conflicts)
docker-compose exec redis redis-cli FLUSHALL

# Check PostgreSQL is ready
docker-compose logs postgres

# Wait for databases to be ready (30-60 seconds)
sleep 30
```

#### 3. Build and Run the Application
```bash
# Make gradlew executable (Linux/Mac)
chmod +x gradlew

# Build the application
./gradlew build

# Run with development profile
./gradlew bootRun --args='--spring.profiles.active=dev'
```

#### 4. Verify Installation
```bash
# Check application health
curl http://localhost:8080/actuator/health

# Test tenant isolation
curl -H "X-Tenant-ID: tenant-a" http://localhost:8080/api/users
curl -H "X-Tenant-ID: tenant-b" http://localhost:8080/api/users
```

### Running Tests
```bash
# Run all tests with test containers
./gradlew test

# Run with coverage report
./gradlew test jacocoTestReport
```

### Monitoring and Metrics
- **Application Health**: http://localhost:8080/actuator/health
- **Prometheus Metrics**: http://localhost:8080/actuator/prometheus
- **Prometheus UI**: http://localhost:9090

## API Usage Examples

### User Management (PostgreSQL)
```bash
# Create user for tenant A
curl -X POST -H "Content-Type: application/json" -H "X-Tenant-ID: tenant-a" \
  -d '{"name":"John Doe","email":"john@example.com"}' \
  http://localhost:8080/api/users

# Get users for tenant A
curl -H "X-Tenant-ID: tenant-a" http://localhost:8080/api/users

# Get specific user
curl -H "X-Tenant-ID: tenant-a" http://localhost:8080/api/users/1
```

### Product Management (MongoDB)
```bash
# Create product for tenant B
curl -X POST -H "Content-Type: application/json" -H "X-Tenant-ID: tenant-b" \
  -d '{"name":"Laptop","description":"Gaming laptop","price":1299.99}' \
  http://localhost:8080/api/products

# Get products for tenant B
curl -H "X-Tenant-ID: tenant-b" http://localhost:8080/api/products
```

## Using as a Scaffolding Project

This project is designed to serve as a robust foundation for building scalable multi-tenant data services. Here's how to extend it:

### 1. Adding New Entities

**For Relational Data (PostgreSQL):**
```java
// 1. Create entity in model package
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String tenantId;
    // ... other fields
}

// 2. Create repository with tenant-aware queries
public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("SELECT o FROM Order o WHERE o.tenantId = :tenantId")
    List<Order> findByTenantId(@Param("tenantId") String tenantId);
}

// 3. Create service with caching
@Service
public class OrderService {
    @Cacheable(value = "orders", key = "#root.methodName + ':' + T(com.dataservices.tenant.TenantContext).getTenantId()")
    public List<Order> getAllOrders() {
        return orderRepository.findByTenantId(TenantContext.getTenantId());
    }
}

// 4. Create REST controller
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    // Standard CRUD operations
}
```

**For Document Data (MongoDB):**
```java
// Follow the same pattern as Product entity
@Document(collection = "analytics")
public class Analytics {
    @Id
    private String id;
    private String tenantId;
    // ... document fields
}
```

### 2. Extending Multi-Tenancy

**Database-per-Tenant Pattern:**
```java
@Configuration
public class MultiTenantDatabaseConfig {
    @Bean
    public DataSource dataSource() {
        // Implement tenant-specific database routing
        return new TenantRoutingDataSource();
    }
}
```

**Enhanced Security:**
```java
// Add JWT-based tenant identification
@Component
public class JwtTenantResolver {
    public String extractTenantFromToken(String jwt) {
        // Extract tenant from JWT claims
    }
}
```

### 3. Adding New Data Stores

**Example: Adding Elasticsearch:**
```gradle
// build.gradle
implementation 'org.springframework.boot:spring-boot-starter-data-elasticsearch'
```

```java
// Configuration
@Configuration
@EnableElasticsearchRepositories
public class ElasticsearchConfig {
    // Elasticsearch client configuration
}

// Repository
public interface SearchRepository extends ElasticsearchRepository<SearchDocument, String> {
    List<SearchDocument> findByTenantId(String tenantId);
}
```

### 4. Microservices Decomposition

**Service Extraction Pattern:**
```yaml
# docker-compose.yml - Add new microservice
user-service:
  build: ./user-service
  ports:
    - "8081:8080"
  depends_on:
    - postgres

product-service:
  build: ./product-service  
  ports:
    - "8082:8080"
  depends_on:
    - mongodb
```

### 5. Advanced Features to Add

**Event Sourcing:**
```java
@Entity
public class DomainEvent {
    private String tenantId;
    private String eventType;
    private String aggregateId;
    private String eventData;
    private LocalDateTime createdAt;
}
```

**CQRS Implementation:**
```java
// Command side
@Service
public class CommandHandler {
    public void handle(CreateUserCommand command) {
        // Write operations
    }
}

// Query side  
@Service
public class QueryHandler {
    @Cacheable("user-projections")
    public UserProjection getUser(String id) {
        // Optimized read operations
    }
}
```

**Distributed Tracing:**
```gradle
implementation 'io.micrometer:micrometer-tracing-bridge-brave'
implementation 'io.zipkin.reporter2:zipkin-reporter-brave'
```

### 6. Production Readiness Checklist

- [ ] **Security**: Add authentication/authorization (OAuth2, JWT)
- [ ] **Rate Limiting**: Implement per-tenant rate limits
- [ ] **Circuit Breakers**: Add resilience patterns with Resilience4j
- [ ] **Distributed Tracing**: Add Zipkin/Jaeger integration
- [ ] **Database Migrations**: Use Flyway/Liquibase for schema management
- [ ] **API Documentation**: Add OpenAPI/Swagger documentation
- [ ] **Load Testing**: Performance testing with tenant isolation
- [ ] **Backup Strategy**: Implement tenant-aware backup procedures

## Deployment

### Docker Deployment
```bash
# Build application image
./gradlew build
docker build -t scalable-data-services .

# Deploy full stack
docker-compose up -d
```

### Kubernetes Deployment
```yaml
# Add Kubernetes manifests in k8s/ directory
apiVersion: apps/v1
kind: Deployment
metadata:
  name: scalable-data-services
spec:
  replicas: 3
  selector:
    matchLabels:
      app: scalable-data-services
  template:
    spec:
      containers:
      - name: app
        image: scalable-data-services:latest
        ports:
        - containerPort: 8080
```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

- **Documentation**: Check the code comments and this README
- **Issues**: Use GitHub Issues for bug reports and feature requests
- **Discussions**: Use GitHub Discussions for questions and community support

---

**Built with ❤️ for scalable, multi-tenant data architectures**