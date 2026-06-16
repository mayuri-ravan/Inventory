<<<<<<< HEAD
# Smart Inventory & Order Management System

Enterprise-level Spring Boot 3 REST API for multi-role inventory, order, supplier, and warehouse management.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Framework | Spring Boot 3.2.3 |
| Security | Spring Security + JWT (jjwt 0.12.3) |
| Database | MySQL 8.0 / JPA + Hibernate |
| Mapping | MapStruct 1.5.5 |
| Boilerplate | Lombok |
| Docs | Springdoc OpenAPI 3 (Swagger UI) |
| Build | Maven 3.9+ |
| Java | 17 |

---

## Roles & Permissions

| Role | Capabilities |
|------|-------------|
| ADMIN | Full access: manage users, approve products, all operations |
| MANAGER | Manage staff, approve/reject products, view all orders |
| STAFF | Manage inventory/stock, update order status |
| SUPPLIER | Create/update own products, manage supplier profile |
| CUSTOMER | Browse products, place/view/cancel own orders |

---

## Prerequisites

- Java 17+
- Maven 3.9+
- MySQL 8.0+ (running locally or via Docker)

---

## Quick Start

### 1. Create MySQL Database

```sql
CREATE DATABASE smart_inventory CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Or run the provided schema:
```bash
mysql -u root -p < src/main/resources/schema.sql
```

### 2. Configure Database

Edit `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/smart_inventory?useSSL=false&serverTimezone=UTC&createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
```

### 3. Build & Run

```bash
# Build (skip tests for quick start)
mvn clean package -DskipTests

# Run
java -jar target/smart-inventory-1.0.0.jar
```

Or with Maven:
```bash
mvn spring-boot:run
```

### 4. Verify

The app starts on `http://localhost:8080/api`

- **Swagger UI:** http://localhost:8080/api/swagger-ui.html
- **Health check:** http://localhost:8080/api/actuator/health

---

## Default Credentials (auto-seeded on first run)

```
Username : admin
Password : Admin@12345
Role     : ADMIN
```
⚠️ **Change this password in production!**

---

## API Overview

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/auth/login | Login → get JWT tokens |
| POST | /api/auth/register | Customer self-registration |
| POST | /api/auth/refresh | Refresh access token |

### Users
| Method | Endpoint | Roles | Description |
|--------|----------|-------|-------------|
| GET | /api/users/me | ALL | Get own profile |
| GET | /api/users | ADMIN, MANAGER | List users |
| POST | /api/users/manager | ADMIN | Create Manager |
| POST | /api/users/staff | ADMIN, MANAGER | Create Staff |
| POST | /api/users/supplier-user | ADMIN | Create Supplier user |
| PUT | /api/users/{id}/status | ADMIN | Enable/disable user |

### Products
| Method | Endpoint | Roles | Description |
|--------|----------|-------|-------------|
| GET | /api/products | PUBLIC | Browse products |
| POST | /api/products | SUPPLIER | Submit product |
| PUT | /api/products/{id} | SUPPLIER, ADMIN, MANAGER | Update product |
| PUT | /api/products/{id}/approve | ADMIN, MANAGER | Approve product |
| PUT | /api/products/{id}/reject | ADMIN, MANAGER | Reject product |

### Inventory
| Method | Endpoint | Roles | Description |
|--------|----------|-------|-------------|
| POST | /api/inventory/stock | ADMIN, MANAGER, STAFF | Update stock |
| GET | /api/inventory/product/{id} | ADMIN, MANAGER, STAFF | Stock by product |
| GET | /api/inventory/warehouse/{id} | ADMIN, MANAGER, STAFF | Stock by warehouse |
| GET | /api/inventory/low-stock | ADMIN, MANAGER, STAFF | Low stock alerts |
| GET | /api/inventory/movements | ADMIN, MANAGER, STAFF | Audit log |

### Orders
| Method | Endpoint | Roles | Description |
|--------|----------|-------|-------------|
| POST | /api/orders | CUSTOMER | Place order |
| GET | /api/orders/my-orders | CUSTOMER | Own orders |
| GET | /api/orders | ADMIN, MANAGER, STAFF | All orders |
| PUT | /api/orders/{id}/status | ADMIN, MANAGER, STAFF | Update status |
| PUT | /api/orders/{id}/cancel | CUSTOMER, ADMIN, MANAGER | Cancel order |

---

## Sample cURL Commands

### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"admin","password":"Admin@12345"}'
```

### Use token for authenticated requests
```bash
TOKEN="<paste_access_token_here>"

# List products
curl -X GET "http://localhost:8080/api/products?status=APPROVED&page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"

# Create warehouse
curl -X POST http://localhost:8080/api/warehouses \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"East Warehouse","location":"456 East Zone","description":"Secondary warehouse"}'

# Update stock (inward)
curl -X POST http://localhost:8080/api/inventory/stock \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"productId":1,"warehouseId":1,"quantity":100,"movementType":"INWARD","remarks":"Initial stock"}'

# Place order (as customer)
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "items": [{"productId": 1, "quantity": 2}],
    "shippingAddress": "789 Customer Lane, City",
    "notes": "Please deliver in the morning"
  }'
```

---

## Typical Workflow

1. **Admin logs in** → gets JWT
2. **Admin creates Supplier user** → `POST /api/users/supplier-user`
3. **Supplier logs in** → creates profile → `POST /api/suppliers/profile`
4. **Supplier submits product** → `POST /api/products`
5. **Admin/Manager approves product** → `PUT /api/products/{id}/approve`
6. **Staff adds stock** → `POST /api/inventory/stock` (INWARD)
7. **Customer registers** → `POST /api/auth/register`
8. **Customer places order** → `POST /api/orders` (stock auto-deducted)
9. **Staff updates order status** → `PUT /api/orders/{id}/status`
10. **View audit log** → `GET /api/inventory/movements`

---

## Project Structure

```
src/main/java/com/smartinventory/
├── SmartInventoryApplication.java
├── config/          # Security, CORS, Swagger, Auditing, DataInitializer
├── controller/      # REST controllers (Auth, User, Product, Supplier, Warehouse, Inventory, Order)
├── dto/
│   ├── request/     # Input DTOs with validation
│   └── response/    # Output DTOs
├── entity/          # JPA entities
├── enums/           # Role, ProductStatus, OrderStatus, StockMovementType
├── exception/       # Custom exceptions + GlobalExceptionHandler
├── repository/      # Spring Data JPA repositories
├── security/        # JWT utilities + filter + UserDetailsService
├── service/         # Service interfaces
│   └── impl/        # Service implementations
└── util/            # AppUtils (current user, order number generation)
```

---

## Security Notes

- All passwords hashed with BCrypt (strength 12)
- JWT tokens are stateless — no server-side session
- CORS configured via `application.properties`
- SQL injection prevented via JPA parameterized queries
- Input validated with `jakarta.validation` on all DTOs
- Role-based access via `@PreAuthorize` on each endpoint

---

## Logs

- Application logs: `logs/smart-inventory.log`
- Audit logs: `logs/audit.log`
- Console: coloured output with timestamp, thread, level, class

=======
# Inventory
Inventory
>>>>>>> bd6917815c0ade90aa8dd2990ea486abfe88beef
