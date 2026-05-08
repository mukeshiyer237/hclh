# Backend Service — Engineering Handbook

Spring Boot 4 · Java 17 · PostgreSQL · Flyway · Kafka (Redpanda) · JWT · JaCoCo · SonarCloud

This document is the single source of truth for every engineer working on this service.
Read it end-to-end once. Refer back to specific sections as you build.

---

## Table of Contents

1. [Project overview](#1-project-overview)
2. [Repository rules — what never gets committed](#2-repository-rules--what-never-gets-committed)
3. [Local setup](#3-local-setup)
4. [Running the application](#4-running-the-application)
5. [Running tests](#5-running-tests)
6. [Project structure](#6-project-structure)
7. [How to add a new API end-to-end](#7-how-to-add-a-new-api-end-to-end)
8. [Database — creating a new table](#8-database--creating-a-new-table)
9. [Logging — how and where](#9-logging--how-and-where)
10. [Exception handling](#10-exception-handling)
11. [Kafka — publishing and consuming events](#11-kafka--publishing-and-consuming-events)
12. [Writing tests](#12-writing-tests)
13. [Feature toggles](#13-feature-toggles)
14. [Team workflow — trunk-based development and pull requests](#14-team-workflow--trunk-based-development-and-pull-requests)
15. [SonarCloud and coverage gate](#15-sonarcloud-and-coverage-gate)
16. [Maven command reference](#16-maven-command-reference)
17. [Environment variable reference](#17-environment-variable-reference)

---

## 1. Project overview

This service is the production template from which all internal APIs are built.
It provides:

- **JWT authentication** — stateless, HMAC-SHA256 signed, loaded from env var secret
- **Role-based access control** — `@PreAuthorize` on any method or controller
- **Flyway schema management** — every DB change is a versioned migration script, never `ddl-auto: create`
- **Structured audit logging** — every authenticated API call writes a row to `audit_log`
- **Kafka publishing** — single `KafkaProducerService` entry point for all outbound events
- **Global exception handling** — all errors return a consistent JSON `ApiError` shape
- **JaCoCo coverage gate** — build fails if line coverage drops below 60%
- **SonarCloud analysis** — wired, runs on `mvn verify sonar:sonar`

Profiles:

| Profile | When used | DB | Kafka fail-fast |
|---|---|---|---|
| `local` | Local development | Render or local PostgreSQL | `false` |
| `test` | `mvn test` | H2 in-memory | `false` |
| `prod` | Deployed service | Render PostgreSQL | `true` |

---

## 2. Repository rules — what never gets committed

### `.env` is gitignored. It must stay that way.

```
.env          ← gitignored. Contains real credentials. NEVER commit this.
.env.example  ← committed. Contains placeholder values. Always keep it in sync.
```

The `.gitignore` already blocks `.env`. Do not add exceptions. Do not rename it to
`application-secrets.yml` and commit it. Do not paste credentials into `application.yml`
or any YAML file. Do not hardcode secrets anywhere in Java code.

**If a secret is committed by mistake:**
1. Rotate the credential immediately — treat it as compromised.
2. Remove it from git history with `git filter-repo` (not `git rm` — that leaves it in history).
3. Force-push only after team coordination. Inform the team.

### What else stays out of git

```
target/           ← compiled output
*.log             ← log files
logs/             ← rolling log directory
*.iml / .idea/    ← IDE files
.vscode/settings  ← personal editor config (workspace settings are fine if committed intentionally)
```

### `.env.example` discipline

Every time you add a new env var to your `.env`, you **must** also add it to `.env.example`
with a placeholder value and a comment. This is how new team members know what variables exist.

```bash
# Good
DB_PASSWORD=your_postgres_password_here   # Required. Get from team vault.

# Bad
DB_PASSWORD=                              # Tells nobody anything
```

---

## 3. Local setup

### Prerequisites

- Java 17 (verify: `java -version`)
- Maven 3.9+ (verify: `mvn -version`)
- Access to the shared Render PostgreSQL instance **or** a local PostgreSQL database
- Access to the Redpanda Kafka cluster credentials (from team vault)

### First-time setup

**Step 1 — Copy the env template**

```bash
cp .env.example .env
```

**Step 2 — Fill in your `.env`**

Open `.env` and replace every placeholder. The required values:

```
DB_URL=jdbc:postgresql://<host>:<port>/<dbname>
DB_USERNAME=<username>
DB_PASSWORD=<password>
JWT_SECRET=<minimum 32 character hex string>
KAFKA_BOOTSTRAP_SERVERS=<host:port>
KAFKA_SASL_JAAS_CONFIG=org.apache.kafka.common.security.scram.ScramLoginModule required username="<u>" password="<p>";
```

To generate a JWT secret:
```bash
# On any machine with openssl
openssl rand -hex 32
```

**Step 3 — Load env vars into your shell (PowerShell)**

```powershell
Get-Content ".env" | Where-Object { $_ -notmatch "^\s*#" -and $_ -match "=" } | `
  ForEach-Object { $k,$v = $_ -split "=",2; [System.Environment]::SetEnvironmentVariable($k.Trim(), $v.Trim(), "Process") }
```

On Mac/Linux (bash/zsh):
```bash
export $(grep -v '^#' .env | xargs)
```

You need to do this once per terminal session. The app reads env vars at startup.

---

## 4. Running the application

**Always load `.env` first** (see Step 3 above), then:

```powershell
mvn spring-boot:run
```

The app starts on `http://localhost:8080`. Look for this in the log:

```
Started DemoApplication in 30.x seconds
```

### Confirming it works

```bash
# Health check
curl http://localhost:8080/actuator/health

# Register a user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","email":"alice@example.com","password":"Password1!"}'

# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"Password1!"}'
```

The login response contains a `token`. Use it as `Authorization: Bearer <token>` on all
protected endpoints.

### Swagger UI

Available at `http://localhost:8080/swagger-ui.html` in local profile.

---

## 5. Running tests

Tests run against **H2 in-memory** — they do not touch the real database or Kafka.
No credentials are needed. Run them clean, any time.

```powershell
# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=AuthServiceTest

# Run a single test method
mvn test -Dtest=AuthServiceTest#register_happyPath_returnsTokenAndUsername

# Run tests + generate JaCoCo coverage report + enforce 60% gate
mvn clean verify

# View the HTML coverage report after verify
# Open in browser: target/site/jacoco/index.html
```

The test profile is activated automatically — `@ActiveProfiles("test")` is on
`AbstractIntegrationTest` and `DemoApplicationTests`. You do not need to set it manually.

**Coverage gate**: `mvn verify` will fail with `BUILD FAILURE` if line coverage drops below 60%.
This is enforced by JaCoCo. Do not skip it. Do not lower the threshold without a team decision.

---

## 6. Project structure

```
src/
├── main/
│   ├── java/com/example/demo/
│   │   ├── audit/              # AuditInterceptor, AuditLog entity, AuditLogRepository
│   │   ├── config/             # SecurityConfig, KafkaConfig, CorsConfig, WebMvcConfig, OpenApiConfig
│   │   ├── controller/         # REST controllers
│   │   │   └── dto/            # Request and response record classes
│   │   ├── domain/
│   │   │   └── entity/         # JPA entities (User, BaseEntity)
│   │   ├── exception/          # GlobalExceptionHandler, ApiError, ResourceNotFoundException
│   │   ├── kafka/
│   │   │   └── producer/       # KafkaProducerService
│   │   ├── repository/         # Spring Data JPA interfaces
│   │   ├── security/           # JwtService, JwtAuthFilter, SecurityUser, UserDetailsServiceImpl
│   │   ├── service/            # Business logic (AuthService, future services)
│   │   └── util/               # Constants
│   └── resources/
│       ├── application.yml         # Base config — shared across all profiles
│       ├── application-local.yml   # Local overrides (show-sql, debug logging)
│       ├── application-prod.yml    # Production overrides
│       ├── logback-spring.xml      # Logging config — console (local) / file rolling (prod)
│       └── db/migration/           # Flyway SQL scripts (V1__, V2__, ...)
└── test/
    ├── java/com/example/demo/
    │   ├── AbstractIntegrationTest.java    # Base class for Spring context tests
    │   ├── MockJwtFactory.java             # Generates test JWTs
    │   ├── TestDataFactory.java            # Shared test data builders
    │   ├── controller/                     # Controller unit tests
    │   ├── repository/                     # Repository integration tests (H2)
    │   ├── service/                        # Service unit tests
    │   └── kafka/                          # Kafka producer tests
    └── resources/
        └── application-test.yml            # H2, embedded Kafka, short JWT expiry
```

### The rule for where things live

| What you're writing | Where it goes |
|---|---|
| HTTP endpoint | `controller/` |
| Request body shape | `controller/dto/` as a `record` |
| Response body shape | `controller/dto/` as a `record` |
| Business logic | `service/` |
| Database query | `repository/` as a Spring Data interface method |
| JPA entity (maps to a DB table) | `domain/entity/` |
| DB schema change | `db/migration/` as a new `V{n}__description.sql` |
| Kafka event publishing | Call `KafkaProducerService.publish()` from the service layer |
| Kafka event consuming | `kafka/consumer/` (create this package when needed) |
| App-wide config bean | `config/` |
| Shared test utilities | `test/.../TestDataFactory.java` or a new factory class |

---

## 7. How to add a new API end-to-end

This section walks through adding a complete `Product` API (`GET`, `POST`, `DELETE`).
Follow this pattern for every new resource.

### Step 1 — Create the Flyway migration

Create `src/main/resources/db/migration/V3__products.sql`:

```sql
-- V3: Products table
CREATE TABLE IF NOT EXISTS products (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    price       NUMERIC(19,4) NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100)
);

CREATE INDEX idx_products_active ON products (active);
```

**Rules:**
- Filename format is strict: `V{n}__{snake_case_description}.sql`. Two underscores.
- Never edit a migration that has already run on any shared environment. Add a new one instead.
- Never use `CREATE TABLE` without `IF NOT EXISTS`.
- Always include indexes for columns you will filter or join on.

### Step 2 — Create the entity

`src/main/java/com/example/demo/domain/entity/Product.java`:

```java
package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
```

**Rules:**
- Always extend `BaseEntity` — it provides `createdAt`, `updatedAt`, `createdBy`, `updatedBy` for free via JPA auditing.
- Use `BigDecimal` for all money. Never `double` or `float`. Ever.
- Column lengths and nullability in the entity must match the migration exactly.

### Step 3 — Create the repository

`src/main/java/com/example/demo/repository/ProductRepository.java`:

```java
package com.example.demo.repository;

import com.example.demo.domain.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByActiveTrue();

    // Spring Data derives the query from the method name.
    // For complex queries use @Query("SELECT p FROM Product p WHERE ...")
}
```

### Step 4 — Create the DTOs

`src/main/java/com/example/demo/controller/dto/CreateProductRequest.java`:

```java
package com.example.demo.controller.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CreateProductRequest(
    @NotBlank @Size(min = 1, max = 200) String name,
    @NotNull @DecimalMin("0.01")        BigDecimal price
) {}
```

`src/main/java/com/example/demo/controller/dto/ProductResponse.java`:

```java
package com.example.demo.controller.dto;

import java.math.BigDecimal;

public record ProductResponse(Long id, String name, BigDecimal price, boolean active) {}
```

**Rules:**
- Use Java `record` for all DTOs. Immutable, compact, no boilerplate.
- Validation annotations (`@NotBlank`, `@NotNull`, `@Size`, etc.) go on the **request** DTO only.
- Response DTOs never expose internal entity fields (no `createdBy`, no JPA internals).
- Never pass the entity object directly to the controller response.

### Step 5 — Create the service

`src/main/java/com/example/demo/service/ProductService.java`:

```java
package com.example.demo.service;

import com.example.demo.controller.dto.CreateProductRequest;
import com.example.demo.controller.dto.ProductResponse;
import com.example.demo.domain.entity.Product;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.kafka.producer.KafkaProducerService;
import com.example.demo.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private static final String TOPIC_PRODUCTS = "products-topic";

    private final ProductRepository    productRepository;
    private final KafkaProducerService kafkaProducerService;

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return productRepository.findByActiveTrue().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        return toResponse(product);
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        Product product = Product.builder()
                .name(request.name())
                .price(request.price())
                .build();

        Product saved = productRepository.save(product);
        log.info("Product created: id={} name={}", saved.getId(), saved.getName());

        kafkaProducerService.publish(TOPIC_PRODUCTS, "product-created", saved);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        product.setActive(false);          // soft delete — never hard delete financial records
        productRepository.save(product);
        log.info("Product soft-deleted: id={}", id);
    }

    private ProductResponse toResponse(Product p) {
        return new ProductResponse(p.getId(), p.getName(), p.getPrice(), p.isActive());
    }
}
```

**Rules:**
- All DB-writing methods must be `@Transactional`. All read-only methods must be `@Transactional(readOnly = true)`. Never omit this.
- Never throw `IllegalArgumentException` for business rule violations. Use `ResourceNotFoundException` (404) or create a domain-specific exception.
- Soft delete (set `active = false`) instead of hard delete for anything that touches financial data.
- The service is the only layer that talks to the repository. Controllers never touch repositories directly.

### Step 6 — Create the controller

`src/main/java/com/example/demo/controller/ProductController.java`:

```java
package com.example.demo.controller;

import com.example.demo.controller.dto.CreateProductRequest;
import com.example.demo.controller.dto.ProductResponse;
import com.example.demo.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAll() {
        return ResponseEntity.ok(productService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

**Rules:**
- Controllers are thin. They translate HTTP → service call → HTTP. No business logic here.
- Always use `@Valid` on `@RequestBody`. Without it, validation annotations on the DTO are ignored.
- Use `@PreAuthorize` to declare who can call the endpoint. `hasRole('ADMIN')` checks for `ROLE_ADMIN`.
- Return `ResponseEntity` with the explicit status code. Do not return raw objects.

---

## 8. Database — creating a new table

Every schema change follows this checklist:

```
1. Create a new migration file:  src/main/resources/db/migration/V{n}__description.sql
2. Test locally:                 mvn spring-boot:run  (Flyway auto-runs pending migrations on startup)
3. Verify migration applied:     mvn flyway:info -Dflyway.url=... -Dflyway.user=... -Dflyway.password=...
4. Commit the migration file in its own commit.
5. Never edit a file once it has run on shared/prod DB.
```

### Naming conventions

| Object | Convention | Example |
|---|---|---|
| Migration file | `V{n}__{verb}_{noun}.sql` | `V4__add_accounts_table.sql` |
| Table name | `snake_case`, plural | `accounts`, `audit_logs` |
| Column name | `snake_case` | `account_number`, `created_at` |
| Index name | `idx_{table}_{column(s)}` | `idx_accounts_user_id` |
| FK constraint | `fk_{child}_{parent}` | `fk_accounts_users` |

### Fixing a bad migration (checksums)

If you edited a migration file after it already ran on any shared DB:

```powershell
# Load env first, then:
mvn flyway:repair `
  -Dflyway.url="$env:DB_URL" `
  -Dflyway.user="$env:DB_USERNAME" `
  -Dflyway.password="$env:DB_PASSWORD" `
  -Dflyway.locations="classpath:db/migration"
```

`repair` fixes mismatched checksums. It does not undo schema changes.

### Nuclear reset (development only — destroys all data)

```powershell
mvn flyway:clean flyway:migrate `
  -Dflyway.url="$env:DB_URL" `
  -Dflyway.user="$env:DB_USERNAME" `
  -Dflyway.password="$env:DB_PASSWORD" `
  -Dflyway.locations="classpath:db/migration" `
  -Dflyway.cleanDisabled="false"
```

**Never run `flyway:clean` against prod or any shared DB.**

---

## 9. Logging — how and where

### Setting up a logger

Every class that needs to log gets the Lombok `@Slf4j` annotation. That is all.

```java
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PaymentService {
    // log is now available — no field declaration needed
}
```

### Log level guide

| Level | When to use | Example |
|---|---|---|
| `log.error(...)` | Unrecoverable failures, unexpected exceptions | DB write failed, external service down |
| `log.warn(...)` | Expected-but-bad situations, degraded state | JWT validation failed, resource not found |
| `log.info(...)` | Significant business events | User registered, payment processed, order created |
| `log.debug(...)` | Developer diagnostic data (local only — suppressed in prod) | Entering method, intermediate state |

### How to log correctly

```java
// CORRECT — use parameterised placeholders, never string concatenation
log.info("Payment processed: userId={} amount={} currency={}", userId, amount, currency);
log.warn("JWT validation failed for request: uri={}", request.getRequestURI());
log.error("Failed to publish event to topic [{}]: {}", topic, ex.getMessage(), ex);

// WRONG — string concatenation builds the string even when the log level is suppressed
log.debug("Processing payment for user: " + userId + " amount: " + amount);  // never do this

// WRONG — logging sensitive data
log.info("User login: username={} password={}", username, password);  // NEVER log passwords
log.info("JWT token generated: {}", token);                           // NEVER log tokens
log.info("Card number: {}", cardNumber);                              // NEVER log PAN data
```

### What must never appear in logs

- Passwords, secrets, API keys, JWT tokens
- Full card numbers, CVVs, account numbers
- Personal identification data (NRIC, passport numbers)
- Raw stack traces exposed to end users (they go to the log file, not the API response)

---

## 10. Exception handling

### The pattern

All exceptions flow through `GlobalExceptionHandler`. You do not need to `try/catch` in
controllers or services for handled exception types — just throw and let the handler deal with it.

```java
// In a service — throw the right exception type, not a generic one
public Product findById(Long id) {
    return productRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
}
```

The handler returns this JSON shape for every error:

```json
{
  "timestamp": "2026-05-08T14:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Product not found: 42",
  "path": "/api/v1/products/42"
}
```

### Adding a new exception type

When your domain needs a new error (e.g. 409 Conflict for a duplicate), create a dedicated
exception class and register a handler:

**Step 1 — Create the exception**

```java
// src/main/java/com/example/demo/exception/DuplicateResourceException.java
package com.example.demo.exception;

public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
```

**Step 2 — Add a handler in `GlobalExceptionHandler`**

```java
@ExceptionHandler(DuplicateResourceException.class)
public ResponseEntity<ApiError> handleDuplicate(DuplicateResourceException ex,
                                                HttpServletRequest request) {
    log.warn("Duplicate resource: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(
        ApiError.builder()
            .status(HttpStatus.CONFLICT.value())
            .error("Conflict")
            .message(ex.getMessage())
            .path(request.getRequestURI())
            .build());
}
```

**Step 3 — Use it in the service**

```java
if (productRepository.existsByName(request.name())) {
    throw new DuplicateResourceException("Product already exists: " + request.name());
}
```

### Exception hierarchy for this codebase

| HTTP status | Exception class | When to throw |
|---|---|---|
| 400 | `MethodArgumentNotValidException` | Auto-thrown by Spring when `@Valid` fails |
| 401 | Handled at filter level | Never throw manually |
| 403 | `AccessDeniedException` | Auto-thrown by Spring Security |
| 404 | `ResourceNotFoundException` | Entity not found in DB |
| 409 | `DuplicateResourceException` | Unique constraint violation (add this) |
| 500 | Any uncaught `Exception` | Falls through to the catch-all |

---

## 11. Kafka — publishing and consuming events

### Publishing an event

Inject `KafkaProducerService` and call `publish`. That is the only way to publish from
application code.

```java
@Service
@RequiredArgsConstructor
public class TransactionService {

    private static final String TOPIC_TRANSACTIONS = "transactions-topic";

    private final KafkaProducerService kafkaProducerService;

    public void processPayment(Payment payment) {
        // ... business logic ...

        // Publish without a key (Kafka round-robins across partitions)
        kafkaProducerService.publish(TOPIC_TRANSACTIONS, payment);

        // Publish with a key — messages with the same key always go to the same partition.
        // Use this when ordering matters for a given entity (e.g. all events for userId=42
        // must be processed in order).
        kafkaProducerService.publish(TOPIC_TRANSACTIONS, payment.getUserId().toString(), payment);
    }
}
```

The payload is any Java object. `KafkaProducerService` serializes it to JSON before sending.

### Declaring a new topic

Add it in `KafkaConfig.java`:

```java
@Value("${kafka.topics.transactions:transactions-topic}")
private String transactionsTopic;

@Bean
public NewTopic transactionsTopic() {
    return TopicBuilder.name(transactionsTopic)
            .partitions(3)
            .replicas(1)       // increase to 3 in production
            .build();
}
```

### Consuming events

Create a consumer class in `src/main/java/com/example/demo/kafka/consumer/`:

```java
package com.example.demo.kafka.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TransactionEventConsumer {

    @KafkaListener(topics = "${kafka.topics.transactions:transactions-topic}",
                   groupId = "${spring.kafka.consumer.group-id}")
    public void onMessage(String message) {
        log.info("Received transaction event: {}", message);
        // Deserialize and process the message here.
        // Do not let unchecked exceptions propagate — they will cause endless redelivery.
        try {
            // process...
        } catch (Exception ex) {
            log.error("Failed to process transaction event: {}", ex.getMessage(), ex);
            // Dead-letter the message or alert — do not silently swallow.
        }
    }
}
```

**Rules:**
- Consumer methods must never throw unchecked exceptions without a dead-letter strategy.
  An unhandled exception causes Kafka to redeliver the message indefinitely.
- Always log the raw message before deserialization for debugging.
- Topic names must come from config (`${kafka.topics.xxx}`), not hardcoded strings.

---

## 12. Writing tests

### Test class categories

| Category | Base class / annotation | DB | Kafka | When to use |
|---|---|---|---|---|
| Unit test | `@ExtendWith(MockitoExtension.class)` | None | None | Services, utilities |
| Controller unit test | `@ExtendWith(MockitoExtension.class)` | None | None | Controllers (no Spring context) |
| Integration test | extends `AbstractIntegrationTest` | H2 | Embedded | Repositories, full flows |

### Unit test — service

```java
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock ProductRepository    productRepository;
    @Mock KafkaProducerService kafkaProducerService;
    @InjectMocks ProductService productService;

    @Test
    void findById_existingProduct_returnsResponse() {
        Product product = Product.builder().id(1L).name("Widget").price(new BigDecimal("9.99")).build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Widget");
        verify(productRepository).findById(1L);
    }

    @Test
    void findById_missingProduct_throwsResourceNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("99");
    }
}
```

### Integration test — repository

```java
class ProductRepositoryTest extends AbstractIntegrationTest {

    @Autowired ProductRepository productRepository;

    @Test
    void findByActiveTrue_returnsOnlyActiveProducts() {
        productRepository.save(Product.builder().name("Active").price(BigDecimal.ONE).active(true).build());
        productRepository.save(Product.builder().name("Inactive").price(BigDecimal.ONE).active(false).build());

        List<Product> results = productRepository.findByActiveTrue();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Active");
    }
}
```

`AbstractIntegrationTest` is `@Transactional` — every test rolls back automatically.
You do not need to clean up data.

### Test naming convention

```
methodName_scenarioBeingTested_expectedOutcome

findById_existingProduct_returnsResponse
findById_missingProduct_throwsResourceNotFoundException
create_duplicateName_throwsDuplicateResourceException
```

### What must be tested for every new service method

1. Happy path — returns expected result
2. Not found — throws `ResourceNotFoundException`
3. Duplicate / constraint violation — throws the correct exception
4. Kafka publish is called (verify with `verify(kafkaProducerService).publish(...)`)

---

## 13. Feature toggles

Feature toggles allow incomplete features to be merged to `main` without being active in
production. This enables trunk-based development (see Section 14).

### Implementation pattern

Define a property in `application.yml`:

```yaml
features:
  products-api: false    # flip to true when ready to enable
  new-payment-flow: false
```

Create a config class:

```java
package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "features")
public record FeatureFlags(
    boolean productsApi,
    boolean newPaymentFlow
) {}
```

Register it in the main application class:

```java
@EnableConfigurationProperties(FeatureFlags.class)
@SpringBootApplication
public class DemoApplication { ... }
```

Guard the feature in the controller or service:

```java
@Service
@RequiredArgsConstructor
public class ProductService {

    private final FeatureFlags features;

    public List<ProductResponse> findAll() {
        if (!features.productsApi()) {
            throw new ResourceNotFoundException("Products API is not available");
            // or: return Collections.emptyList();
        }
        // ... real logic
    }
}
```

To enable the feature on a specific environment, set the env var:

```bash
FEATURES_PRODUCTS_API=true
```

Spring Boot maps `FEATURES_PRODUCTS_API` → `features.products-api` automatically.

### Why this matters for the team

- Engineers can merge partial implementations daily without fear of breaking production.
- QA can enable a feature on staging by flipping an env var, not by deploying a branch.
- If a feature causes issues in production, it can be disabled in seconds without a redeploy.

---

## 14. Team workflow — trunk-based development and pull requests

### The model

This team uses **trunk-based development**. There is one main branch: `main`.

- Feature branches live for **at most 2 days**. If it takes longer, the feature needs feature toggles.
- Direct commits to `main` are reserved for trivial fixes only.
- All other work goes through a pull request, no matter how small.

### Day-to-day workflow

```bash
# 1. Always start from an up-to-date main
git checkout main
git pull origin main

# 2. Create a short-lived feature branch
git checkout -b feature/TICKET-123-add-products-api

# 3. Commit small and often — each commit should build and pass tests
git add src/main/java/com/example/demo/service/ProductService.java
git commit -m "feat(products): add ProductService with findAll and create"

# 4. Keep your branch rebased on main daily
git fetch origin
git rebase origin/main

# 5. Push and open a PR
git push origin feature/TICKET-123-add-products-api
```

### Commit message format

```
type(scope): short description

type:  feat | fix | refactor | test | docs | chore
scope: the module or domain area (products, auth, kafka, db)

Examples:
feat(products): add ProductService with findAll and create
fix(auth): return 409 on duplicate username instead of 500
test(products): add unit tests for ProductService
chore(deps): bump jjwt to 0.12.7
db(migration): add V3 products table
```

### Pull request checklist

Before opening a PR, verify all of the following:

```
[ ] mvn clean verify passes locally (all tests green, coverage gate met)
[ ] New code has tests — service, controller, and repository where applicable
[ ] No credentials or secrets in any committed file
[ ] .env.example updated if new env vars were added
[ ] Migration file added if schema changed (and tested locally)
[ ] Feature toggle added if the feature is not complete
[ ] Log statements use parameterised placeholders, not string concatenation
[ ] No sensitive data in log statements
[ ] @Transactional annotation on all service write methods
[ ] @Valid on all @RequestBody parameters
[ ] API response uses a DTO record, not the raw entity
```

### PR size rules

| PR size | Lines changed | Policy |
|---|---|---|
| Small | < 200 | Preferred. Review same day. |
| Medium | 200–500 | Acceptable. Must have clear description. |
| Large | > 500 | Split it. If you cannot, explain why in the PR description. |

Large PRs get reviewed slowly and have a higher chance of introducing bugs that reviewers miss.
Keep PRs small by using feature toggles for incomplete work.

### Review etiquette

- Reviewer approves or requests changes within **one business day**.
- Author addresses all comments before merging — no self-merge on outstanding questions.
- `Approved + no outstanding comments` = merge. Use **squash merge** to keep `main` history clean.
- Do not merge your own PR unless it is a hotfix and you have verbal approval.

---

## 15. SonarCloud and coverage gate

### Local coverage report

```powershell
# Run tests + generate report
mvn clean verify

# Open the HTML report
# target/site/jacoco/index.html
```

### Push analysis to SonarCloud

Fill in your `.env`:
```
SONAR_ORGANIZATION=your-org-key
SONAR_PROJECT_KEY=your-project-key
SONAR_TOKEN=your-token
```

Then:
```powershell
# Load env, then:
mvn verify sonar:sonar
```

Go to `https://sonarcloud.io` to see the full report: bugs, vulnerabilities, code smells,
duplications, and coverage breakdown by class.

### Coverage gate

The build fails if line coverage drops below **60%**. This threshold lives in `pom.xml`
under the JaCoCo `check` execution. Raising it requires a team decision — do not lower it.

Excluded from coverage (boilerplate, not testable logic):
- `config/**`
- `domain/entity/**`
- `controller/dto/**`
- `*Application.java`
- `DiagnosticMain.java`

---

## 16. Maven command reference

```powershell
# ── Build ─────────────────────────────────────────────────────────────────────

# Compile only (no tests, no JAR)
mvn compile

# Full build with tests
mvn clean package

# Full build skipping tests (CI build when tests run separately)
mvn clean package -DskipTests

# ── Run ───────────────────────────────────────────────────────────────────────

# Run locally (load .env first)
mvn spring-boot:run

# Run the packaged JAR
java -jar target/demo-0.0.1-SNAPSHOT.jar

# ── Test ──────────────────────────────────────────────────────────────────────

# Run all tests
mvn test

# Run one test class
mvn test -Dtest=ProductServiceTest

# Run one test method
mvn test -Dtest=ProductServiceTest#findById_existingProduct_returnsResponse

# Run tests + coverage report + enforce gate
mvn clean verify

# ── Database ──────────────────────────────────────────────────────────────────

# (Set these once per session after loading .env)
# $url   = $env:DB_URL
# $user  = $env:DB_USERNAME
# $pass  = $env:DB_PASSWORD
# $loc   = "classpath:db/migration"

# Show migration status
mvn flyway:info -Dflyway.url="$env:DB_URL" -Dflyway.user="$env:DB_USERNAME" -Dflyway.password="$env:DB_PASSWORD" -Dflyway.locations="classpath:db/migration"

# Validate checksums
mvn flyway:validate -Dflyway.url="$env:DB_URL" -Dflyway.user="$env:DB_USERNAME" -Dflyway.password="$env:DB_PASSWORD" -Dflyway.locations="classpath:db/migration"

# Fix checksum mismatch after editing a migration
mvn flyway:repair -Dflyway.url="$env:DB_URL" -Dflyway.user="$env:DB_USERNAME" -Dflyway.password="$env:DB_PASSWORD" -Dflyway.locations="classpath:db/migration"

# Run pending migrations manually
mvn flyway:migrate -Dflyway.url="$env:DB_URL" -Dflyway.user="$env:DB_USERNAME" -Dflyway.password="$env:DB_PASSWORD" -Dflyway.locations="classpath:db/migration"

# Nuclear reset — wipe and re-run all migrations (DEV ONLY)
mvn flyway:clean flyway:migrate -Dflyway.url="$env:DB_URL" -Dflyway.user="$env:DB_USERNAME" -Dflyway.password="$env:DB_PASSWORD" -Dflyway.locations="classpath:db/migration" -Dflyway.cleanDisabled="false"

# ── Dependencies ──────────────────────────────────────────────────────────────

# Show full dependency tree
mvn dependency:tree

# Check a specific artifact
mvn dependency:tree -Dincludes=org.postgresql:*

# ── SonarCloud ────────────────────────────────────────────────────────────────

# Run analysis (load .env with SONAR_* vars first)
mvn verify sonar:sonar
```

---

## 17. Environment variable reference

Copy `.env.example` to `.env` and fill in every value.

| Variable | Required | Description |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Yes | `local` for development, `prod` for production |
| `SERVER_PORT` | No | Defaults to `8080` |
| `DB_URL` | Yes | Full JDBC URL: `jdbc:postgresql://host:port/dbname` |
| `DB_USERNAME` | Yes | PostgreSQL username |
| `DB_PASSWORD` | Yes | PostgreSQL password |
| `JWT_SECRET` | Yes | Min 32 characters. Generate with `openssl rand -hex 32` |
| `JWT_EXPIRATION_MS` | No | Token lifetime in ms. Defaults to `86400000` (24h) |
| `KAFKA_BOOTSTRAP_SERVERS` | Yes | Kafka/Redpanda broker address: `host:port` |
| `KAFKA_CONSUMER_GROUP` | No | Defaults to `demo-group` |
| `KAFKA_SECURITY_PROTOCOL` | No | `SASL_SSL` for Redpanda Cloud, `PLAINTEXT` for local |
| `KAFKA_SASL_MECHANISM` | No | `SCRAM-SHA-256` for Redpanda |
| `KAFKA_SASL_JAAS_CONFIG` | Yes (if SASL) | Full JAAS config string — see `.env.example` |
| `SONAR_ORGANIZATION` | No | SonarCloud org key — needed only for `sonar:sonar` |
| `SONAR_PROJECT_KEY` | No | SonarCloud project key |
| `SONAR_TOKEN` | No | SonarCloud auth token |

---

*Last updated: May 2026*
