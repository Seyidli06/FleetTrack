# FleetTrack

FleetTrack is a production-aware **Vehicle Fleet Management System** built with Java and Spring Boot.

The project goes beyond basic CRUD by combining fleet operations with transactional business rules, concurrency control, Redis caching, rate limiting, scheduled processing, real-time GPS updates over WebSocket, PDF reporting, and JWT-based role authorization.

## Features

- Vehicle management
- Driver management and license tracking
- Driver-to-vehicle assignment lifecycle
- Vehicle maintenance scheduling and history
- Maintenance lifecycle management
- Dynamic filtering, sorting, and pagination
- Cursor-based pagination for high-volume GPS history
- Real-time vehicle location updates with WebSocket/STOMP
- Redis caching
- Redis Pub/Sub notifications
- Scheduled maintenance reminders
- Asynchronous background processing
- PDF maintenance reports
- JWT authentication with RSA signatures
- Role-Based Access Control
- Redis-backed API rate limiting
- Standardized RFC-style API error responses
- Flyway database migrations
- Optimistic and pessimistic concurrency control
- PostgreSQL-level business constraints
- Testcontainers-based integration testing
- Production-oriented health checks and configuration

---

## Tech Stack

| Area | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1.1 |
| Web | Spring Web MVC |
| Security | Spring Security, OAuth2 Resource Server |
| Authentication | JWT / RSA / RS256 |
| Database | PostgreSQL 16 |
| ORM | Spring Data JPA / Hibernate |
| Database Migration | Flyway |
| Cache | Redis |
| Messaging | Redis Pub/Sub |
| Real-Time | WebSocket / STOMP |
| Mapping | MapStruct |
| Validation | Jakarta Bean Validation |
| API Documentation | Springdoc OpenAPI / Swagger |
| PDF | Apache PDFBox |
| Testing | JUnit, Spring Test, Testcontainers |
| Build | Maven |
| Infrastructure | Docker Compose |

---

## Architecture

FleetTrack is implemented as a **modular monolith organized by feature**.

```text
src/main/java/com/fleettrack
│
├── assignment
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── repository
│   └── service
│
├── auth
├── driver
├── location
├── maintenance
├── notification
├── ratelimit
├── report
├── security
├── vehicle
│
├── common
│   ├── cache
│   ├── dto
│   ├── error
│   └── exception
│
└── config
    ├── async
    ├── cache
    ├── openapi
    ├── ratelimit
    ├── redis
    ├── scheduling
    └── websocket
```

Business logic lives primarily in the service layer, while controllers focus on HTTP concerns and repositories handle persistence.

Cross-cutting infrastructure such as security, caching, scheduling, Redis, rate limiting, and WebSocket configuration is kept separate from core business features.

---

## Core Domain

### Vehicles

Fleet vehicles contain information such as:

- VIN
- License plate
- Make
- Model
- Manufacture year
- Operational status

Supported lifecycle statuses include:

```text
ACTIVE
IN_MAINTENANCE
OUT_OF_SERVICE
RETIRED
```

VIN and license plate uniqueness are enforced case-insensitively at the PostgreSQL level.

---

### Drivers

Driver profiles include:

- First name
- Last name
- License number
- License category
- License expiry date
- Contact information
- Driver status

A driver with an expired license may remain in the system but cannot be assigned to a vehicle.

---

### Vehicle Assignments

Driver-to-vehicle relationships are modeled using a dedicated assignment history instead of storing a driver directly on the vehicle.

An assignment contains:

```text
vehicle
driver
assignedAt
unassignedAt
```

Only one active driver may be assigned to a vehicle at a time.

Likewise, a driver may only have one active vehicle assignment.

These rules are enforced both in the application layer and through PostgreSQL partial unique indexes.

---

### Maintenance

FleetTrack models maintenance as a lifecycle:

```text
SCHEDULED
    │
    ├──> IN_PROGRESS
    │       │
    │       ├──> COMPLETED
    │       └──> CANCELLED
    │
    └──> CANCELLED
```

Only one maintenance record may be `IN_PROGRESS` for a vehicle at the same time.

Starting maintenance automatically places the vehicle into:

```text
IN_MAINTENANCE
```

Completing or cancelling an active maintenance operation restores the vehicle to:

```text
ACTIVE
```

where applicable.

---

## REST API

All business endpoints are versioned under:

```text
/api/v1
```

### Authentication

```http
POST /api/v1/auth/login
```

Returns an RSA-signed JWT access token.

---

### Vehicles

```http
POST   /api/v1/vehicles
GET    /api/v1/vehicles
GET    /api/v1/vehicles/{id}
PUT    /api/v1/vehicles/{id}
DELETE /api/v1/vehicles/{id}
```

Vehicle searches support filtering, pagination, and whitelisted sorting.

Example:

```http
GET /api/v1/vehicles?status=ACTIVE&make=Toyota&page=0&size=20&sort=createdAt,desc
```

---

### Drivers

```http
POST   /api/v1/drivers
GET    /api/v1/drivers
GET    /api/v1/drivers/{id}
PUT    /api/v1/drivers/{id}
DELETE /api/v1/drivers/{id}
```

Driver filtering includes:

- status
- first name
- last name
- license category
- license expiration range

---

### Assignments

```http
POST   /api/v1/vehicles/{vehicleId}/assignments
GET    /api/v1/vehicles/{vehicleId}/assignments
GET    /api/v1/vehicles/{vehicleId}/assignments/current
DELETE /api/v1/vehicles/{vehicleId}/assignments/current
```

Assignment history is preserved after a driver is unassigned.

---

### Maintenance

```http
POST   /api/v1/vehicles/{vehicleId}/maintenance
GET    /api/v1/vehicles/{vehicleId}/maintenance

GET    /api/v1/maintenance/{id}
PUT    /api/v1/maintenance/{id}
PATCH  /api/v1/maintenance/{id}/status
DELETE /api/v1/maintenance/{id}
```

---

### Vehicle Locations

```http
POST /api/v1/vehicles/{vehicleId}/locations
GET  /api/v1/vehicles/{vehicleId}/locations/latest
GET  /api/v1/vehicles/{vehicleId}/locations
```

Location history uses cursor-based pagination instead of deep offset pagination.

Example:

```http
GET /api/v1/vehicles/1/locations?size=20
```

A subsequent request can use:

```http
GET /api/v1/vehicles/1/locations?cursorRecordedAt=...&cursorId=...&size=20
```

Results are deterministically ordered by:

```text
recordedAt DESC
id DESC
```

---

### Reports

```http
GET /api/v1/reports/vehicles/{vehicleId}/maintenance.pdf
```

Returns a generated PDF containing vehicle information and maintenance history.

---

## Security

FleetTrack uses Spring Security with stateless JWT authentication.

### JWT

Tokens are signed using:

```text
RS256
```

The application uses:

- PKCS#8 RSA private key
- X.509 RSA public key
- issuer validation
- expiration validation
- role claims

Example token claims:

```json
{
  "iss": "fleettrack",
  "sub": "admin",
  "roles": [
    "ADMIN"
  ]
}
```

### Roles

Current application roles:

```text
ADMIN
FLEET_MANAGER
```

Typical fleet operations are available to both roles, while selected destructive operations require `ADMIN`.

---

## WebSocket Security

The WebSocket handshake endpoint is available at:

```text
/ws
```

Authentication is performed during STOMP `CONNECT` using:

```text
Authorization: Bearer <token>
```

Clients may subscribe only to authorized vehicle-location destinations:

```text
/topic/vehicles/{vehicleId}/location
```

Client-side STOMP `SEND` commands are rejected.

---

## Real-Time GPS Updates

When a location is recorded:

```text
REST Request
    │
    ▼
VehicleLocationService
    │
    ▼
PostgreSQL transaction
    │
    ▼
COMMIT
    │
    ▼
VehicleLocationCreatedEvent
    │
    ▼
WebSocket Publisher
    │
    ▼
/topic/vehicles/{id}/location
```

WebSocket publication occurs **after the database transaction commits**, preventing clients from receiving location data that was later rolled back.

---

## Redis Caching

Redis caching is used for frequently accessed resources.

Current caches include:

```text
vehicleById
driverById
latestVehicleLocation
```

Example TTL strategy:

| Cache | TTL |
|---|---:|
| Vehicle | 10 minutes |
| Driver | 10 minutes |
| Latest Location | 30 seconds |

Cache operations are transaction-aware.

Redis cache failures use a fail-open strategy so a temporary cache outage does not make core database-backed functionality unavailable.

---

## Rate Limiting

FleetTrack includes a Redis-backed fixed-window rate limiter.

Default policies:

| Scope | Limit |
|---|---:|
| Login | 5 requests / minute |
| General API | 120 requests / minute |
| Reports | 10 requests / minute |

Responses include rate-limit headers such as:

```text
X-RateLimit-Limit
X-RateLimit-Remaining
X-RateLimit-Reset
Retry-After
```

Exceeded limits return:

```http
HTTP 429 Too Many Requests
```

with an `application/problem+json` response.

Login rate limiting uses a fail-closed policy when Redis is unavailable, while normal authenticated traffic favors availability and fails open.

---

## Standardized API Errors

FleetTrack uses Spring `ProblemDetail` for standardized error responses.

Example:

```json
{
  "title": "Resource not found",
  "status": 404,
  "detail": "The requested resource was not found.",
  "instance": "/api/v1/vehicles/999",
  "code": "RESOURCE_NOT_FOUND",
  "timestamp": "2026-09-05T16:33:42Z"
}
```

Application error codes include:

```text
VALIDATION_ERROR
MALFORMED_REQUEST
BAD_REQUEST
UNAUTHORIZED
FORBIDDEN
RESOURCE_NOT_FOUND
DUPLICATE_RESOURCE
BUSINESS_RULE_CONFLICT
STALE_VERSION
DATA_INTEGRITY_CONFLICT
RATE_LIMIT_EXCEEDED
RATE_LIMIT_UNAVAILABLE
INTERNAL_ERROR
```

Database error details and internal stack traces are never exposed to API clients.

---

## Database Design

PostgreSQL is the authoritative data store.

Hibernate schema generation is disabled:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

Database evolution is managed using Flyway.

Current migrations include:

```text
V1  Initial baseline
V2  Core fleet schema
V3  Core indexes
V4  Authentication schema
V5  Maintenance concurrency constraints
V6  Query-performance indexes
V7  Driver trigram search indexes
V8  Case-insensitive identifiers
V9  Vehicle-location pagination optimization
```

### Important Database Constraints

FleetTrack does not rely exclusively on application-side validation.

PostgreSQL also protects critical invariants such as:

- unique VIN
- unique license plate
- unique driver license number
- valid vehicle statuses
- valid driver statuses
- valid maintenance statuses
- latitude between -90 and 90
- longitude between -180 and 180
- non-negative odometer
- non-negative maintenance cost
- valid maintenance date ranges
- one active assignment per vehicle
- one active assignment per driver
- one `IN_PROGRESS` maintenance per vehicle

---

## Query Optimization

Indexes are designed around actual access patterns.

Examples include:

```text
vehicles(created_at DESC)
drivers(created_at DESC)

maintenance_records(
    status,
    next_service_date
)

vehicle_locations(
    vehicle_id,
    recorded_at DESC,
    id DESC
)
```

Driver name contains-search uses PostgreSQL:

```text
pg_trgm
```

with GIN trigram indexes.

Case-insensitive identifiers use expression indexes such as:

```sql
UPPER(vin)
UPPER(license_plate)
UPPER(license_number)
```

---

## Concurrency Control

FleetTrack uses multiple concurrency strategies depending on the use case.

### Optimistic Locking

Version fields protect mutable resources such as vehicles and maintenance records.

Clients send the current version when updating a resource.

Stale writes return:

```http
HTTP 409 Conflict
```

### Pessimistic Locking

Selected state transitions use database write locks where serialized updates are necessary.

### Database Constraints

Critical race-sensitive invariants are also enforced using partial unique indexes.

This ensures correctness even if concurrent requests pass application-level pre-checks simultaneously.

---

## Scheduled Maintenance Reminders

A scheduled job scans upcoming maintenance deadlines.

Default schedule:

```text
08:00 Asia/Baku
```

The scheduler retrieves maintenance candidates and dispatches notification work asynchronously.

A dedicated bounded executor is used:

```text
core threads: 2
max threads: 4
queue capacity: 100
```

`CallerRunsPolicy` provides backpressure instead of silently dropping tasks.

---

## Redis Pub/Sub

Maintenance reminder events are published through Redis Pub/Sub.

This separates scheduled business processing from downstream notification handling and provides a simple event-driven integration point.

---

## Running Locally

### Requirements

Install:

- Java 21
- Docker Desktop
- Git

The project includes the Maven Wrapper, so a separate Maven installation is not required.

---

### 1. Clone the Repository

```bash
git clone <repository-url>
cd FleetTrack
```

---

### 2. Start PostgreSQL and Redis

```bash
docker compose up -d
```

Default local ports:

```text
PostgreSQL: localhost:5433
Redis:      localhost:6380
Backend:    localhost:8081
```

---

### 3. Generate RSA Keys

Create the following directory:

```text
keys/
```

The application expects:

```text
keys/private.pem
keys/public.pem
```

The private key must be PKCS#8 encoded.

The public key must use X.509 encoding.

Do not commit private keys to Git.

---

### 4. Bootstrap an Admin User

On first local startup, set bootstrap credentials.

PowerShell example:

```powershell
$env:BOOTSTRAP_ADMIN_ENABLED="true"
$env:BOOTSTRAP_ADMIN_USERNAME="admin"
$env:BOOTSTRAP_ADMIN_EMAIL="admin@fleettrack.local"
$env:BOOTSTRAP_ADMIN_PASSWORD="change-this-password"
```

Start the application:

```powershell
.\mvnw.cmd spring-boot:run
```

After the admin user has been created, disable bootstrap:

```powershell
$env:BOOTSTRAP_ADMIN_ENABLED="false"
```

The existing admin remains stored in PostgreSQL.

---

## Build

Run all tests and package the application:

```powershell
.\mvnw.cmd clean package
```

The Spring Boot Maven plugin creates an executable JAR.

Run it with:

```powershell
java -jar .\target\FleetTrack-0.0.1-SNAPSHOT.jar
```

---

## Production Profile

FleetTrack includes a dedicated:

```text
application-prod.yaml
```

Production configuration includes:

- externalized database credentials
- configurable HikariCP pool sizing
- Redis connection and command timeouts
- graceful shutdown
- Tomcat thread limits
- disabled bootstrap admin
- disabled Swagger by default
- hidden exception details
- health probes
- configurable rate limiting

Start using:

```powershell
$env:SPRING_PROFILES_ACTIVE="prod"
$env:DB_URL="jdbc:postgresql://localhost:5433/fleettrack"
$env:DB_USERNAME="fleettrack"
$env:DB_PASSWORD="<database-password>"
$env:REDIS_HOST="localhost"
$env:REDIS_PORT="6380"

java -jar .\target\FleetTrack-0.0.1-SNAPSHOT.jar
```

Production bootstrap admin is disabled by default.

---

## Health Checks

Available actuator endpoints include:

```http
GET /actuator/health
GET /actuator/health/liveness
GET /actuator/health/readiness
```

Readiness intentionally depends on PostgreSQL because the primary application cannot operate correctly without its database.

Redis is treated as a degradable dependency for selected features.

---

## API Documentation

During local development:

```text
Swagger UI:
/swagger-ui.html

OpenAPI JSON:
/v3/api-docs
```

Swagger and API docs are disabled by default in the production profile.

---

## Testing

Run the test suite with:

```powershell
.\mvnw.cmd test
```

or:

```powershell
.\mvnw.cmd clean test
```

Integration tests use Testcontainers with real PostgreSQL and Redis instances.

Important test areas include:

- application context startup
- Flyway schema validation
- maintenance concurrency
- stale-version handling
- database constraint races
- Redis rate limiting
- HTTP 429 behavior
- Redis outage behavior

Concurrency tests intentionally execute competing database transactions to verify that PostgreSQL constraints protect business invariants under race conditions.

---

## Design Decisions

### Why a Modular Monolith?

FleetTrack currently benefits from:

- simple deployment
- transactional consistency
- low operational complexity
- clear feature boundaries

Splitting the system into microservices at this stage would introduce infrastructure complexity without providing enough business value.

The feature-oriented structure still allows individual modules to evolve independently.

### Why PostgreSQL Constraints in Addition to Java Validation?

Application-level checks alone are not sufficient under concurrent requests.

Critical rules are therefore also enforced in PostgreSQL.

Example:

```text
Request A ─┐
           ├── both see "no active assignment"
Request B ─┘

           ↓

PostgreSQL partial unique index

           ↓

only one transaction succeeds
```

### Why Cursor Pagination for GPS History?

Vehicle-location tables can become substantially larger than ordinary fleet tables.

Deep offset pagination becomes increasingly expensive because the database still needs to traverse skipped rows.

Cursor pagination uses the indexed ordering:

```text
recorded_at DESC
id DESC
```

and scales much better for historical GPS data.

---



## Project Status

Core FleetTrack functionality is implemented.

The project currently includes:

```text
Vehicle management
Driver management
Assignments
Maintenance lifecycle
Filtering and pagination
GPS tracking
WebSocket updates
Redis caching
Scheduled reminders
Redis Pub/Sub
PDF reporting
JWT/RBAC
Rate limiting
Flyway migrations
Concurrency protection
Integration testing
Production configuration
```

