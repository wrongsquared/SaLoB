# AGENTS.md - Food Service Microservice

## Project Overview
A Spring Boot 4.0.6 microservice (Java 25) for managing food establishments and menu entries with geospatial features. Part of the SaLoB backend system.

## Architecture & Core Patterns

### Domain-Driven Structure
- **`domain/`**: Pure JPA entities with business semantics
  - `BaseEntity`: All entities inherit UUID primary key + auto-audited `createdAt` timestamp
  - Core entities: `Eatery`, `Food`, `FoodEntry`, `EateryType`, `FoodEntryVote`, `FoodEntryMedia`, `EateryClosureFlag`
  - Uses `@CreatedDate` and `AuditingEntityListener` for automatic timestamp management (configured in `JpaConfig`)

- **`api/`**: Repository interfaces and data transfer logic
  - Spring Data JPA repositories use declaration-based query fragments (e.g., `existsByName()`, `findByLabel()`)
  - Future home for controllers, DTOs, mappers, and exception handlers (currently empty dirs)

- **`seeding/`**: Deterministic database initialization
  - `SeedDataRunner` triggers on `@Profile("dev")` + `@ConditionalOnProperty(prefix="app.seed", name="enabled")`
  - `EateryTypeSeeder` → `EaterySeeder` (explicit ordering required; seeders use `existsBy*` checks for idempotency)
  - Exposes 80+ Singapore locations with GPS coordinates using PostGIS SRID 4326 (WGS84)

### Geospatial Integration (PostGIS)
- Eatery locations stored as `Point` geometry with `columnDefinition = "geometry(Point, 4326)"`
- Uses JTS `GeometryFactory(PrecisionModel(), 4326)` to construct coordinates (longitude, latitude order)
- Critical for distance-based queries; always provide coordinates in (lon, lat) order

### Foreign Key & Cascade Patterns
- `Eatery` → `List<FoodEntry>` and `List<EateryClosureFlag>`: `cascade = CascadeType.ALL, orphanRemoval = true`
- `FoodEntry` → `List<FoodEntryVote>` and `List<FoodEntryMedia>`: same cascade strategy
- Implies: deleting an eatery auto-deletes its food entries, votes, and media

### Transaction & Lazy Loading
- All seeders use `@Transactional` to ensure ACID guarantees and manage lazy-loaded collections
- Most relationships use `FetchType.LAZY` to avoid N+1 queries (important for `Eatery.foodEntries`)

## Critical Developer Workflows

### Local Development Setup
```bash
# Copy example config
cp .env.example .env

# Build + run tests with embedded testcontainers (PostgreSQL, RabbitMQ, Redis)
./gradlew clean build

# Run app in dev mode (triggers seeding if app.seed.enabled=true)
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Environment Configuration
- App loads from `.env` via Spring's `spring.config.import: "optional:file:.env[.properties]"`
- Required vars: `POSTGRES_*`, `REDIS_*`, `RABBITMQ_*`, `APP_PORT`, `SPRING_SECURITY_USER_*`
- Service runs on `${APP_PORT}` (default 8083); database on `${POSTGRES_HOST}:${POSTGRES_PORT}`

### Database Migrations
- Flyway auto-runs on startup via `spring.jpa.flyway.enabled: true`
- DDL managed by Hibernate on startup (`spring.jpa.hibernate.ddl-auto: update`)
- Migrations stored in `src/main/resources/db/migration/` (currently empty; schema built from JPA entities)

### Testing with Testcontainers
- `TestcontainersConfiguration` spins up docker containers for PostgreSQL, RabbitMQ, Redis
- `@ServiceConnection` auto-discovers container ports and configures Spring datasources
- `TestFoodServiceApplication.main()` runs app with test config for integration tests

## Project-Specific Patterns & Conventions

### Lombok Usage
- All entities use `@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`
- Seeders use `@RequiredArgsConstructor` for constructor injection
- Omit explicit constructors—Lombok generates them

### UUID Identity
- All entities use `@GeneratedValue(strategy = GenerationType.UUID)` with `@JdbcTypeCode(SqlTypes.UUID)`
- Primary keys immutable: `@Column(updatable = false, nullable = false)`

### Null Safety & Defaults
- Use `@Builder.Default` + field initialization for collection/scalar defaults
  - E.g., `@Builder.Default private List<FoodEntry> foodEntries = new ArrayList<>()`
  - Prevents NPE when hydrating entities without explicit setter calls

### Record Use in Seeders
- `EaterySeeder` uses `record EaterySeedSpec(...)` for immutable seed specifications
- More concise than domain classes for transient data structures

### Security (Placeholders)
- `SecurityConfig` currently permits all requests; OAuth2 resource server configured but inactive
- Will need proper implementation before production deployment

## Integration Points & External Dependencies

### Tech Stack Overview
```
Spring Boot 4.0.6
  ├─ Spring Data JPA + Hibernate ORM + PostGIS adapter
  ├─ Spring Security + OAuth2 Resource Server
  ├─ Spring AMQP (RabbitMQ support)
  ├─ Spring Data Redis
  ├─ Spring WebSocket
  ├─ gRPC with Spring integration (1.0.3)
  ├─ Flyway (schema versioning)
  └─ Validation (Jakarta)

External Services
  ├─ PostgreSQL 16+ (database with PostGIS extension)
  ├─ RabbitMQ 3.x (async messaging)
  ├─ Redis 7.x (caching)
  ├─ MinIO / S3-compatible storage (configured, not yet integrated)
  └─ gRPC services (proto compilation ready)

Testing
  └─ Testcontainers (PostgreSQL, RabbitMQ, Redis auto-provisioning)
```

### Proto/gRPC Setup
- `build.gradle` configures protobuf compiler and gRPC plugin
- Generated sources output to `build/generated/source/proto/main`
- No `.proto` files yet; blueprint is ready in `src/main/proto/`

## Implementation Quick-Start

### Adding a New Entity
1. Create class in `domain/` extending `BaseEntity`
2. Add `@Entity`, `@Table`, JPA annotations (see `Eatery.java` template)
3. Use Lombok: `@Getter`, `@Setter`, `@Builder`, etc.
4. Create repository in `api/<domain>/` extending `JpaRepository<Entity, UUID>`

### Adding a Custom Query
1. Extend repository with declaration-based query: `boolean existsByName(String name);`
2. For complex queries, use `@Query` annotation (not yet seen in this codebase; add as needed)

### Seeding New Data
1. Create seeder class in `seeding/seeders/` with `@Component` and `@Transactional`
2. Implement idempotent `seed()` method using repository existence checks
3. Register in `SeedDataRunner.run()` with explicit ordering
4. Test with `./gradlew bootRun --args='--spring.profiles.active=dev'`

## Known Limitations & Future Work
- Security config incomplete (OAuth2 commented out)
- No REST controllers or gRPC services yet (API layer empty)
- MinIO/S3 integration configured but not wired
- No complex geospatial queries (nearest-neighbor searches, within-radius)
- Proto files not yet created despite gRPC plugin setup

