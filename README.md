
# Standard Inventory System

A Spring Boot web application for managing inventory: items, categories, stock movements, and users. Includes a live dashboard with charts, an audit log for every entity change, and session-based authentication with role-based access.

---

## Features

### Core Inventory
- **Items** — Create, edit, soft-delete, and restore. Search by name/SKU/description. Filter by category, status, low-stock, and out-of-stock. Pagination and sorting.
- **Categories** — Same CRUD lifecycle. Categories can be deactivated without deleting their items.

### Stock Movements
- **Immutable ledger** — every quantity change is a recorded `StockMovement` with type (STOCK_IN / STOCK_OUT / ADJUSTMENT), reason, reference, and the user who performed it.
- **Movement history** — per-item view with before/after quantities and actor.
- **Initial stock** — recorded automatically when an item is created with quantity greater than zero.
- **Guardrails** — stock-out cannot exceed the current quantity; inactive items cannot receive movements.

### Audit Logs
- Every CREATE, UPDATE, DELETE, and RESTORE on items, categories, and users is logged.
- Admin-only `/logs` viewer with filters by entity type, action, user, and date range.
- Logs are immutable — no edit or delete endpoints exist.

### User Management
- Admin-provisioned accounts only (no public registration).
- Force-password-change on first login (`mustChangePassword` flag enforced by an interceptor).
- Soft enable/disable rather than deletion.
- **Last-admin protection** — the final active admin cannot be disabled or demoted.

### Dashboard
- Stat cards: Active Items, Low Stock, Out of Stock.
- **Items by Category** doughnut chart with Count/Value toggle.
- **Stock Movements** line chart with a 7 / 14 / 30 day selector.
- Low-stock alerts, out-of-stock table, and recent movements table.

### Security
- Session-based form login with BCrypt password hashing.
- Role-based access (ADMIN, STAFF) via Spring Security 7 Lambda DSL.
- CSRF protection enabled.
- Custom password-change flow for first-time users.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1.1 |
| Security | Spring Security 7.x |
| Persistence | Spring Data JPA + Hibernate 7 |
| Database | MySQL 8+ |
| Migrations | Flyway |
| Templating | Thymeleaf + Layout Dialect |
| Frontend | MDB 5 (Bootstrap 5 based), Material Symbols |
| Charts | Chart.js 4 |
| Build | Maven |
| Utilities | Lombok |

---

## Prerequisites

- **JDK 21** or newer
- **MySQL 8.0** or newer running locally (or reachable over the network)

---

## Setup

### 1. Create the database and user

Connect to MySQL as an admin and run:

```sql
CREATE DATABASE inventory_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER 'inventory_user'@'localhost' IDENTIFIED BY 'inventory_pass';
GRANT ALL PRIVILEGES ON inventory_db.* TO 'inventory_user'@'localhost';
FLUSH PRIVILEGES;
```

The credentials above match the defaults in `application-dev.yml`. If you use different ones, update that file or override via environment variables.

### 2. Configure the datasource (optional)

The dev profile in `src/main/resources/application-dev.yml` already points at the database created above:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/inventory_db?useSSL=false&serverTimezone=Asia/Manila&allowPublicKeyRetrieval=true
    username: inventory_user
    password: inventory_pass
```

For production, `application-prod.yml` reads credentials from environment variables:

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

### 3. Run the application

```bash
# Linux/macOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

Flyway runs the migrations on startup (`V1` through `V8`), including:
- Schema creation
- Seed admin user
- Demo categories, items, and movements (only on an empty database)

Open http://localhost:8080 in a browser.

### 4. Sign in

| Username | Password | Role |
|---|---|---|
| `admin` | `Admin@123` | ADMIN |

**The first login forces a password change.** Set a new password (minimum 8 characters) to reach the dashboard.

---

## Database Migrations

Flyway migrations live in `src/main/resources/db/migration/`. They run in order on startup.

| Version | Purpose |
|---|---|
| `V1__init_schema.sql` | Creates `users` and `categories` tables |
| `V2__seed_admin.sql` | Seeds the default admin user |
| `V3__create_items.sql` | Creates the `items` table with indexes |
| `V4__create_stock_movements.sql` | Creates the `stock_movements` table |
| `V5__create_inventory_logs.sql` | Creates the `inventory_logs` table |
| `V7__backfill_initial_movements.sql` | Creates initial-stock movements for pre-existing items |
| `V8__seed_demo_data.sql` | Seeds demo categories, items, and movements (only when tables are empty) |

**Never edit a migration that has already run.** Flyway validates checksums and will refuse to start. Add new migrations instead.

**If a migration fails partway** and blocks startup:

```sql
-- Inspect what Flyway recorded
SELECT installed_rank, version, description, success
FROM flyway_schema_history ORDER BY installed_rank;

-- Remove the failed row and re-run
DELETE FROM flyway_schema_history WHERE success = 0;
```

Then drop any partial table the failed migration created and restart.

---

## Configuration

### Profiles

| Profile | File | Purpose |
|---|---|---|
| `dev` | `application-dev.yml` | Local development, hardcoded credentials, `show-sql: true` |
| `prod` | `application-prod.yml` | Environment-variable credentials, `show-sql: false` |

The active profile is set in `application.yml` (`spring.profiles.active: dev`). Override at runtime:

```bash
java -jar target/inventory-system-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### Key settings

| Property | Value | Notes |
|---|---|---|
| `spring.jpa.hibernate.ddl-auto` | `validate` | Hibernate validates entities against the schema; it never alters it |
| `spring.jpa.open-in-view` | `false` | Lazy loading must happen inside service transactions |
| `spring.web.locale` | `en-PH` | Currency formatting uses the Philippine peso |
| `spring.jackson.time-zone` | `Asia/Manila` | Dates render in local time |
| `server.servlet.session.timeout` | `30m` | Session expires after 30 minutes of inactivity |

---

## Running Tests

```bash
./mvnw test
```

The suite covers:
- **ItemServiceTest** — SKU uniqueness, initial-stock recording, quantity is immutable on update, soft-delete/restore rules
- **StockMovementServiceTest** — stock-in/out arithmetic, overdraw rejection, initial-stock skip on zero
- **UserServiceTest** — last-admin protection on disable and demote
- **DashboardServiceTest** — day-filling for the movement chart window
- **InventoryApplicationTests** — context loads

Tests are pure unit tests using Mockito. No database required.

---

## Usage Notes

### Quantity workflow

Quantity is **set only at item creation** via the "Initial Quantity" field, which is recorded as an initial `STOCK_IN` movement. After creation, quantity changes happen exclusively through the movements module (`/items/{id}/movements/new`). The item edit form displays quantity as read-only with a link to record a movement.

This guarantees that every quantity in the database is backed by a movement entry — the ledger and the item always reconcile.

### Soft delete vs hard delete

Items, categories, and users are **soft-deleted** (`active = false` or `enabled = false`). Their audit history and movement records stay intact. Restoring is possible via the row's action button (admin-only for items). Hard deletion is not supported.

### Audit logs

Logs record the entity, the actor, the timestamp, and a human-readable message. They are written inside the same transaction as the mutation — if the mutation rolls back, the log does too.

Stock movements are **not** duplicated into the audit log. Movements are a business ledger; logs are an entity-change trail.

---

## Development Tips

- **Thymeleaf caching** is disabled in the dev profile (`spring.thymeleaf.cache: false`), so template edits appear on the next page load.
- **Static resources** (CSS, JS) are served from `src/main/resources/static/`. Hard-refresh the browser (`Ctrl+Shift+R`) after editing to bypass the cache.
- **Charts** are initialized in `static/js/charts.js`. Chart.js global defaults are set at the top — tweak there to change the theme across all charts.
- **Icons** use Material Symbols. The set of available icon names is at https://fonts.google.com/icons.

---

## Known Limitations

- **No REST API.** The application is server-rendered with Thymeleaf. There is no JSON API for external consumers.
- **No email notifications.** Low-stock alerts are visual only, on the dashboard.
- **No file uploads.** Items do not support images.
- **No multi-warehouse support.** Each item has a single `location` field, but there is no stock-per-location tracking.
- **Concurrency on movements.** Optimistic locking is in place via `@Version` on `Item`. Under heavy concurrent stock updates to the same item, users may see an optimistic-lock error; retrying resolves it.

---

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `Validate failed: Migrations have failed validation` | A migration failed on a previous run | Delete the `success = 0` row from `flyway_schema_history`, drop partial artifacts, restart |
| `LazyInitializationException` when rendering | Missing `JOIN FETCH` in a repository query | Add the fetch for the association the template accesses |
| `Could not bind form errors using expression "global"` | `#fields.hasGlobalErrors()` placed outside the `<form th:object>` | Move the block inside the form element |
| `Method formatCurrency(BigDecimal, String) cannot be found` | Thymeleaf's `#numbers.formatCurrency` accepts an Integer for digits, not a currency code | Use `#numbers.formatCurrency(value)` and let the locale supply the currency |
| Lombok methods not found on compile | Annotation processor not running | `mvn clean compile`; verify `<annotationProcessorPaths>` in `pom.xml` includes Lombok |
| Chart not rendering | Missing data or script load order | Check browser console for `Chart is not defined`; verify `charts.js` loads after Chart.js and `window.dashboardData` is populated in page source |
| `Table 'inventory_db.users' doesn't exist` on startup | Migration order or a stale `baseline-version` | Ensure no `baseline-version` is set in config; drop and recreate the DB if needed |

---

## License

Internal project. Not licensed for external distribution.

---

## Acknowledgements

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Thymeleaf](https://www.thymeleaf.org/)
- [MDBootstrap](https://mdbootstrap.com/)
- [Chart.js](https://www.chartjs.org/)
- [Material Symbols](https://fonts.google.com/icons)
- [Flyway](https://flywaydb.org/)
- [Lombok](https://projectlombok.org/)
