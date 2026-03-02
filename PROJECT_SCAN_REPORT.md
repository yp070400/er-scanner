# ER Scanner - Complete Project Scan Report

**Generated:** March 2, 2026  
**Project:** er-scanner  
**Type:** Spring Boot Application for Database Schema & Relationship Scanning

---

## 📋 PROJECT OVERVIEW

### Purpose
The ER Scanner is a Spring Boot application designed to:
- **Scan database schemas** and extract table structures
- **Detect relationships** between tables (foreign keys, data sample patterns)
- **Generate Entity-Relationship (ER) diagrams** in Mermaid format
- **Analyze and classify** tables into business domains
- **Export schema metadata** in JSON format

### Technology Stack
- **Framework:** Spring Boot 4.0.3
- **Language:** Java 17
- **Database:** MySQL/JDBC
- **Build Tool:** Maven
- **API:** RESTful endpoints with Spring Web

---

## 📁 PROJECT STRUCTURE

```
er-scanner/
├── src/
│   ├── main/
│   │   ├── java/com/yogesh/er_scanner/
│   │   │   ├── ErScannerApplication.java          [Main entry point]
│   │   │   ├── config/
│   │   │   │   └── RelationshipConfig.java         [Configuration for scanning]
│   │   │   ├── controller/
│   │   │   │   └── SchemaController.java           [REST API endpoints]
│   │   │   ├── db/                                [Database utilities]
│   │   │   ├── dto/                               [Data transfer objects]
│   │   │   ├── enum/                              [Enumerations]
│   │   │   ├── model/
│   │   │   │   ├── Column.java                    [Column metadata]
│   │   │   │   ├── Table.java                     [Table metadata]
│   │   │   │   ├── Schema.java                    [Schema container]
│   │   │   │   ├── Relationship.java              [Relationship definition]
│   │   │   │   ├── RelationshipType.java          [STRICT, DATA_SAMPLE]
│   │   │   │   └── ForeignKey.java                [Foreign key metadata]
│   │   │   └── service/
│   │   │       ├── SchemaScanner.java             [Core scanning logic]
│   │   │       ├── SchemaService.java             [Schema processing]
│   │   │       └── DataSampleService.java         [Data pattern analysis]
│   │   └── resources/
│   │       ├── application.yaml                   [Configuration file]
│   │       └── ai-prompt-files/
│   │           └── ENTERPRISEDOMAINDETECTIONPROMPT.txt
│   └── test/                                      [Unit tests]
├── output/
│   ├── ENTERPRISEDOMAINS.json                     [Domain groupings]
│   ├── schema-ai.json                             [Full schema metadata]
│   ├── er_diagram.mmd                             [Mermaid diagram]
│   └── queries.sql                                [Generated SQL queries]
├── pom.xml                                        [Maven configuration]
├── mvnw, mvnw.cmd                                 [Maven wrapper scripts]
└── target/                                        [Compiled artifacts]
    └── er-scanner-0.0.1-SNAPSHOT.jar             [Executable JAR]

```

---

## 🔧 KEY COMPONENTS

### 1. **SchemaScanner Service**
**Location:** `src/main/java/com/yogesh/er_scanner/service/SchemaScanner.java`

**Functionality:**
- Reads database metadata using JDBC DatabaseMetaData API
- Extracts table columns, primary keys, and foreign keys
- Filters tables based on configuration
- Detects STRICT relationship types

**Key Methods:**
- `scan()` - Main scanning method that returns a complete Schema object

**Configuration:**
- Respects `relationship.tables` list from application.yaml
- Enforces maximum table limit via `max-tables` parameter
- Requires relationship scanning to be enabled

### 2. **SchemaService**
**Location:** `src/main/java/com/yogesh/er_scanner/service/SchemaService.java`

**Functionality:**
- Processes scanned schema
- Generates Mermaid ER diagrams
- Builds domain-based diagram chunks
- Exports schema to JSON format

**Output Formats:**
- **JSON:** Complete schema with tables and relationships
- **Mermaid:** Visual ER diagram syntax
- **Domain-based Chunks:** Separate diagrams per business domain

### 3. **SchemaController**
**Location:** `src/main/java/com/yogesh/er_scanner/controller/SchemaController.java`

**REST API Endpoints:**

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/schema/scan` | POST | Trigger full schema scan |
| `/schema/json` | GET | Retrieve schema as JSON |
| `/schema/er-mermaid` | GET | Get complete ER diagram |
| `/schema/er-mermaid-domains` | GET | Get domain-split diagrams |

### 4. **Data Models**

#### **Schema**
Contains collections of:
- `List<Table>` - All scanned tables
- `List<Relationship>` - All detected relationships

#### **Table**
- `name` - Table name
- `columns` - List of Column objects

#### **Column**
- `name` - Column name
- `type` - SQL data type
- `primaryKey` - Boolean flag
- `foreignKey` - Boolean flag

#### **Relationship**
- `sourceTable` - Table with foreign key
- `sourceColumn` - Foreign key column
- `targetTable` - Referenced table
- `targetColumn` - Primary key column
- `relationshipType` - STRICT or DATA_SAMPLE
- `confidence` - Confidence score (0.0-1.0)

#### **RelationshipType**
- `STRICT` - Enforced database constraints
- `DATA_SAMPLE` - Inferred from data patterns

---

## ⚙️ CONFIGURATION

**File:** `src/main/resources/application.yaml`

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3307/er_test
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver

server:
  port: 8080

database:
  type: mysql
  schema: er_test

relationship:
  enabled: true
  sample-size: 20        # Data rows per table for pattern analysis
  max-tables: 100        # Maximum number of tables to scan
  tables:
    - audit_logs
    - categories
    - customers
    - inventory
    - invoices
    - notifications
    - order_items
    - orders
    - payments
    - products
    - roles
    - shipments
    - tenants
    - user_roles
    - users
    # ... plus 39 auxiliary tables (aux_table_2 through aux_table_50)
```

---

## 📊 SCANNED SCHEMA

The application is configured to scan **54 database tables:**

### Business Domain Tables (15)
- **Order Management (5):** orders, order_items, payments, invoices, shipments
- **Customer & User Management (6):** users, tenants, roles, user_roles, notifications, audit_logs
- **Product Catalog (3):** products, categories, inventory
- **Auxiliary Services (39):** aux_table_2 through aux_table_50

### Key Relationships (Detected by Scanner)

#### STRICT Relationships (Enforced Constraints)
1. `audit_logs.user_id` → `users.user_id`
2. `inventory.product_id` → `products.product_id`
3. `invoices.order_id` → `orders.order_id`
4. `notifications.user_id` → `users.user_id`
5. `order_items.order_id` → `orders.order_id`
6. `order_items.product_id` → `products.product_id`
7. `orders.tenant_id` → `tenants.tenant_id`
8. `orders.user_id` → `users.user_id`
9. `payments.order_id` → `orders.order_id`
10. `payments.user_id` → `users.user_id`
11. `products.category_id` → `categories.category_id`
12. `shipments.order_id` → `orders.order_id`
13. `user_roles.role_id` → `roles.role_id`
14. `user_roles.user_id` → `users.user_id`
15. `users.tenant_id` → `tenants.tenant_id`

#### DATA_SAMPLE Relationships (Inferred Patterns)
- 134+ relationships detected through data sample analysis
- Pattern confidence: 100% (1.0)

---

## 📤 OUTPUT ARTIFACTS

### 1. **ENTERPRISEDOMAINS.json**
Classification of all 54 tables into 4 business domains:

```json
{
  "Auxiliary Services": [39 auxiliary tables],
  "Customer & User Management": [6 tables],
  "Order Management": [5 tables],
  "Product Catalog & Inventory": [3 tables]
}
```

### 2. **schema-ai.json**
Complete schema metadata including:
- All 54 tables with column definitions
- 149 detected relationships
- Data types and constraints
- Primary/foreign key information

### 3. **er_diagram.mmd**
Mermaid ER diagram syntax for visualization

### 4. **queries.sql**
Sample SQL query for analyzing customer orders:
```sql
SELECT 
    c.name,
    o.order_id,
    o.amount
FROM customers c
INNER JOIN orders o ON c.customer_id = o.customer_id;
```

---

## 🚀 HOW TO USE

### 1. **Start the Application**
```bash
java -jar target/er-scanner-0.0.1-SNAPSHOT.jar
```

### 2. **Trigger Full Scan**
```bash
curl -X POST http://localhost:8080/schema/scan
```

### 3. **Retrieve Outputs**
```bash
# Get schema JSON
curl http://localhost:8080/schema/json > output/schema.json

# Get ER diagram
curl http://localhost:8080/schema/er-mermaid > output/diagram.mmd

# Get domain-split diagrams
curl http://localhost:8080/schema/er-mermaid-domains > output/domains.json
```

---

## 📝 BUILD & DEPLOYMENT

### Maven Build
```bash
mvn clean package
```

### Run Tests
```bash
mvn test
```

### Package Version
- **Name:** er-scanner-0.0.1-SNAPSHOT
- **Java Version:** 17
- **Spring Boot Version:** 4.0.3

---

## 🎯 KEY FEATURES

✅ **Automated Schema Discovery** - Reads database metadata without manual configuration  
✅ **Relationship Detection** - Identifies both explicit and implicit table relationships  
✅ **Business Domain Classification** - Groups tables into logical business domains  
✅ **Visual Diagram Generation** - Creates Mermaid ER diagrams  
✅ **JSON Export** - Outputs machine-readable schema metadata  
✅ **REST API** - Easy integration with external tools  
✅ **Configurable** - Table lists and scanning parameters via YAML  

---

## 📋 SUMMARY STATISTICS

| Metric | Count |
|--------|-------|
| Total Tables | 54 |
| Business Tables | 15 |
| Auxiliary Tables | 39 |
| STRICT Relationships | 15 |
| DATA_SAMPLE Relationships | 134 |
| Total Relationships | 149 |
| Business Domains | 4 |

---

**Report Generated:** March 2, 2026  
**Status:** Complete Project Scan ✅

