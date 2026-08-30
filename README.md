# Resource Booking System

A RESTful API for booking shared resources (rooms, vehicles, equipment), built with **Spring Boot 3 / Java 17**,
**Spring Security + JWT**, and **JPA/Hibernate** on **PostgreSQL or MySQL**.

- ADMIN: full CRUD on resources and reservations.
- USER: read-only on resources; can create reservations and view/cancel only their own.
- Filtering, pagination, and sorting on the reservations listing endpoint.
- Centralized validation and consistent JSON error responses.
- Swagger/OpenAPI UI + a Postman collection for manual testing.

---

## 1. Tech Stack

| Concern            | Choice                                   |
|---------------------|-------------------------------------------|
| Language / runtime  | Java 17                                   |
| Framework           | Spring Boot 3.3.4                         |
| Security            | Spring Security 6 (stateless) + JJWT 0.12 |
| Persistence         | Spring Data JPA / Hibernate               |
| Database            | PostgreSQL (default) or MySQL             |
| API docs            | springdoc-openapi (Swagger UI)            |
| Build tool           | Maven                                     |

---

## 2. Project Structure

```
src/main/java/com/booking/resourcebooking/
├── config/            SecurityConfig, OpenApiConfig, DataSeeder
├── controller/         AuthController, ResourceController, ReservationController, UserController
├── dto/request/        Request payloads (validated with jakarta.validation)
├── dto/response/        Response payloads
├── entity/             User, Resource, Reservation, Role, ReservationStatus
├── exception/          Custom exceptions + GlobalExceptionHandler
├── repository/         Spring Data JPA repositories (+ Specifications for filtering)
├── security/           JwtUtil, JwtAuthenticationFilter, CurrentUserProvider, ...
├── service/            AuthService, ResourceService, ReservationService
└── specification/      ReservationSpecification (dynamic filtering)
```

---

## 3. Getting Started

### Prerequisites
- JDK 17+
- Maven 3.8+
- PostgreSQL 13+ (or MySQL 8+) — **or** Docker, if you'd rather not install a database locally

### Option A — Docker Compose (fastest)

```bash
docker compose up --build
```

This starts PostgreSQL and the app together. The API is available at `http://localhost:8080`.
Seed users (below) are created automatically on first boot.

### Option B — Run locally against your own PostgreSQL

1. Create the database:
   ```sql
   CREATE DATABASE resource_booking;
   ```
2. Copy `.env.example` to `.env` (or just export the variables) and adjust `DB_USERNAME` / `DB_PASSWORD` if needed.
3. Run:
   ```bash
   mvn spring-boot:run
   ```
   Hibernate will create the schema automatically (`ddl-auto: update`) and `DataSeeder` will insert seed users
   and sample resources on first startup.

### Option C — Run locally against MySQL

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```
This activates `application-mysql.yml`, which points at `jdbc:mysql://localhost:3306/resource_booking`
(edit `DB_USERNAME`/`DB_PASSWORD` env vars, or the profile file, to match your setup — the driver
auto-creates the database via `createDatabaseIfNotExist=true`).

### Running tests

```bash
mvn test
```
Tests run against an in-memory H2 database (`application-test.yml`), so no external DB is needed.

---

## 4. Environment Variables

All variables have sane local defaults baked into `application.yml`, so the app runs out of the box with
zero configuration. Override any of these for real deployments — see `.env.example` for the full list.

| Variable                | Default                                   | Description                                  |
|--------------------------|--------------------------------------------|-----------------------------------------------|
| `SERVER_PORT`            | `8080`                                     | HTTP port                                     |
| `DB_URL`                 | `jdbc:postgresql://localhost:5432/resource_booking` | JDBC URL                            |
| `DB_USERNAME`            | `postgres`                                 | DB username                                   |
| `DB_PASSWORD`            | `postgres`                                 | DB password                                   |
| `DB_DRIVER`              | `org.postgresql.Driver`                    | JDBC driver class                             |
| `DDL_AUTO`               | `update`                                   | Hibernate schema strategy                     |
| `JWT_SECRET`             | *(dev default — change in prod!)*          | Base64-encoded HMAC signing key (256-bit+)     |
| `JWT_EXPIRATION_MS`      | `86400000` (24h)                           | Token lifetime in milliseconds                |
| `SEED_ENABLED`           | `true`                                     | Whether to seed test users/resources on boot  |
| `SEED_ADMIN_USERNAME`    | `admin`                                    | Seed ADMIN username                           |
| `SEED_ADMIN_PASSWORD`    | `Admin@123`                                | Seed ADMIN password                           |
| `SEED_USER_USERNAME`     | `john`                                     | Seed USER username                            |
| `SEED_USER_PASSWORD`     | `User@123`                                 | Seed USER password                            |

> **Production note:** always override `JWT_SECRET` with your own value
> (`openssl rand -base64 32`) and don't rely on `ddl-auto: update` — use a migration tool
> (Flyway/Liquibase) for real deployments.

---

## 5. Seed Users

Created automatically on startup (idempotent — skipped if the username already exists):

| Role  | Username | Password    |
|-------|----------|-------------|
| ADMIN | `admin`  | `Admin@123` |
| USER  | `john`   | `User@123`  |

Plus 5 sample resources (2 rooms, 1 vehicle, 1 piece of equipment, and one room marked unavailable)
so filtering/pagination can be tried immediately.

---

## 6. Authentication

```
POST /auth/login
{
  "username": "admin",
  "password": "Admin@123"
}
```

Response:
```json
{
  "token": "eyJhbGciOi...",
  "tokenType": "Bearer",
  "username": "admin",
  "role": "ADMIN",
  "expiresInMs": 86400000
}
```

Use the token on every subsequent request:
```
Authorization: Bearer <token>
```

`POST /auth/register` self-registers a new **USER** account (role is always forced to `USER` for
self-service signup). To provision an **ADMIN** account, an existing ADMIN calls
`POST /api/users` with an explicit `role`.

---

## 7. Authorization Model (RBAC)

| Action                                  | USER                    | ADMIN |
|-------------------------------------------|--------------------------|-------|
| `GET /api/resources`, `GET /api/resources/{id}` | ✅                | ✅    |
| `POST/PUT/DELETE /api/resources/**`       | ❌ (403)                 | ✅    |
| `POST /api/reservations`                  | ✅ (creates for self only) | ✅ (can book on behalf of any user via optional `userId`) |
| `GET /api/reservations`, `GET /api/reservations/{id}` | ✅ own only        | ✅ all |
| `PUT /api/reservations/{id}` (full edit)  | ❌ (403)                 | ✅    |
| `PATCH /api/reservations/{id}/status`     | ❌ (403)                 | ✅    |
| `PATCH /api/reservations/{id}/cancel`     | ✅ own only              | ✅ any |
| `DELETE /api/reservations/{id}`           | ❌ (403)                 | ✅    |
| `POST /api/users` (create user w/ role)   | ❌ (403)                 | ✅    |

Enforcement happens at two layers:
1. **URL/method-level** — `SecurityConfig` and `@PreAuthorize` annotations block wrong-role requests before they reach a service.
2. **Ownership-level** — `ReservationService` re-derives the caller's identity from `CurrentUserProvider`
   (which reads the authenticated principal out of the JWT-populated `SecurityContext`) and filters/validates
   accordingly. **The `userId` on a reservation is never trusted from a USER's request body** — it always comes
   from the JWT. A USER fetching another user's reservation by ID gets a `404` (not `403`), to avoid leaking
   the existence of other users' bookings.

---

## 8. Reservations: Filtering, Pagination, Sorting

```
GET /api/reservations?status=PENDING&minPrice=10&maxPrice=200&resourceId=1&page=0&size=10&sort=price,desc
```

| Param        | Type    | Notes                                          |
|---------------|---------|-------------------------------------------------|
| `status`      | enum    | `PENDING`, `CONFIRMED`, or `CANCELLED`          |
| `minPrice`    | decimal | inclusive lower bound                           |
| `maxPrice`    | decimal | inclusive upper bound                           |
| `resourceId`  | long    | filter to a single resource                     |
| `page`        | int     | 0-based page index (default `0`)                |
| `size`        | int     | page size (default `20`)                        |
| `sort`        | string  | e.g. `sort=price,desc` or `sort=startTime,asc`  |

A USER's `status`/`price`/`resourceId` filters apply **on top of** an implicit "my reservations only"
scope — they can never see or filter into another user's bookings. An ADMIN's filters apply globally.

Response shape (Spring `Page` JSON):
```json
{
  "content": [ { "id": 1, "resourceId": 1, "resourceName": "Conference Room A", "userId": 2,
                 "username": "john", "startTime": "...", "endTime": "...", "status": "PENDING",
                 "price": 75.00, "notes": "...", "createdAt": "...", "updatedAt": "..." } ],
  "totalElements": 1,
  "totalPages": 1,
  "number": 0,
  "size": 20
}
```

---

## 9. Validation & Error Responses

Bean Validation (`jakarta.validation`) runs on every request body. A `GlobalExceptionHandler`
converts all exceptions into a consistent shape:

```json
{
  "timestamp": "2026-08-30T10:15:00Z",
  "status": 400,
  "error": "Validation Failed",
  "message": "One or more fields are invalid",
  "path": "/api/reservations",
  "details": ["startTime is required", "price must not be negative"]
}
```

| Status | When                                                                 |
|--------|------------------------------------------------------------------------|
| 400    | Bad request body / invalid params / `startTime >= endTime` / bad price range |
| 401    | Missing/invalid/expired JWT, or wrong login credentials                |
| 403    | Authenticated, but role/ownership doesn't allow the action             |
| 404    | Resource/Reservation/User not found (also used to hide other users' reservations from a USER) |
| 409    | Duplicate username/email at registration; overlapping reservation on the same resource; resource marked unavailable |
| 500    | Unexpected server error                                                |

Business rules enforced in the service layer beyond simple field validation:
- `startTime` must be before `endTime`.
- A resource can't have two overlapping non-cancelled reservations (double-booking guard).
- A reservation can't be created against a resource marked `available: false`.
- `minPrice` can't be greater than `maxPrice` in a filter query.

---

## 10. API Documentation

Once the app is running:
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`

Click **Authorize** in Swagger UI and paste the JWT (without the `Bearer` prefix) after logging in via
`POST /auth/login` to try protected endpoints interactively.

A ready-to-import **Postman collection** is also included: [`postman_collection.json`](./postman_collection.json).
It has a `{{token}}` collection variable that auto-populates after running the "Login as Admin" /
"Login as User" requests.

---

## 11. Example Requests

**Create a resource (ADMIN):**
```bash
curl -X POST http://localhost:8080/api/resources \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Meeting Pod 1","type":"ROOM","description":"2-seat pod","location":"2nd Floor","available":true}'
```

**Create a reservation (USER):**
```bash
curl -X POST http://localhost:8080/api/reservations \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"resourceId":1,"startTime":"2026-09-15T10:00:00","endTime":"2026-09-15T12:00:00","price":75.00,"notes":"Planning session"}'
```

**Filter reservations:**
```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/reservations?status=CONFIRMED&minPrice=20&maxPrice=100&page=0&size=5&sort=price,desc"
```

---

## 12. Design Notes / Assumptions

- `Reservation.status` transitions are open (no explicit state machine enforced) beyond blocking
  "cancel an already-cancelled reservation" — this can be tightened with a transition table if
  the business rules require it.
- An ADMIN creating a reservation can optionally pass `userId` in the body to book on behalf of
  another user; this field is silently ignored for USER-role callers so it can never be used to
  spoof identity.
- Passwords are stored using BCrypt; JWTs are signed with HMAC-SHA (HS256) using a Base64-encoded
  secret — swap in an asymmetric key (RS256) if you need multiple services to verify tokens without
  sharing the signing secret.
- `ddl-auto: update` is used for convenience in this take-home-style setup; a real project should
  switch to Flyway/Liquibase migrations and `ddl-auto: validate`.
