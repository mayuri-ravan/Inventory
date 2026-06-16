# Running The Backend

## Requirements

- Java 17 or newer
- Maven 3.9 or newer, unless you run the included jar
- MySQL 8 running locally

## Database

The default database is `smart_inventory`.

Default connection:

```properties
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/smart_inventory?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=root123
```

To override credentials on Windows PowerShell:

```powershell
$env:SPRING_DATASOURCE_USERNAME="root"
$env:SPRING_DATASOURCE_PASSWORD="your_mysql_password"
```

## Start

```powershell
.\start-backend.bat
```

Or manually:

```powershell
mvn clean package -DskipTests
java -jar target\smart-inventory-1.0.0.jar
```

The API runs at `http://localhost:8080/api`.

Swagger UI: `http://localhost:8080/api/swagger-ui.html`

## Default Login

```text
Username: admin
Password: Admin@12345
Role: ADMIN
```

Additional seeded demo users:

```text
manager1  / Manager@123  / ROLE_MANAGER
staff1    / Staff@12345  / ROLE_STAFF
supplier1 / Supplier@123 / ROLE_SUPPLIER
customer1 / Customer@123 / ROLE_CUSTOMER
```

## Enterprise APIs

The upgrade adds dashboard, purchase workflow, notifications, audit logs, reports, profile, settings, password reset, smart analytics, and category intelligence endpoints.

Useful paths:

```text
GET  /api/dashboard/summary
GET  /api/purchase-orders
POST /api/purchase-orders
GET  /api/notifications
GET  /api/smart-analytics
GET  /api/reports/{type}/download?format=CSV|PDF|DOCX|XLSX
GET  /api/audit-logs
PUT  /api/settings
PUT  /api/profile
POST /api/auth/forgot-password
```
