# ER Scanner - Detailed Code Analysis & Architecture

---

## 🏗️ ARCHITECTURE OVERVIEW

### Layered Architecture Pattern
```
┌─────────────────────────────────────┐
│   REST API Layer                    │
│   (SchemaController)                │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│   Service Layer                     │
│   (SchemaService, DataSampleSvc)    │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│   Scanning Layer                    │
│   (SchemaScanner)                   │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│   Data Access Layer                 │
│   (JDBC/DataSource)                 │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│   Database Layer                    │
│   (MySQL/er_test)                   │
└─────────────────────────────────────┘
```

---

## 📦 PACKAGE STRUCTURE

### com.yogesh.er_scanner
**Root package** - Contains main application class

### com.yogesh.er_scanner.config
**Configuration Management**
- `RelationshipConfig.java` - YAML configuration binding
  - Table list configuration
  - Sampling parameters
  - Max table limits
  - Enable/disable flags

### com.yogesh.er_scanner.model
**Domain Models**

#### Table.java
```java
public class Table {
    private String name;
    private List<Column> columns;
    
    // getters/setters
}
```
**Purpose:** Represents a database table with its metadata

#### Column.java
```java
public class Column {
    private String name;
    private String type;
    private boolean primaryKey;
    private boolean foreignKey;
    
    // constructor/getters/setters
}
```
**Purpose:** Represents a column within a table

#### Schema.java
```java
public class Schema {
    private List<Table> tables;
    private List<Relationship> relationships;
}
```
**Purpose:** Container for complete schema metadata

#### Relationship.java
```java
public class Relationship {
    private String sourceTable;
    private String sourceColumn;
    private String targetTable;
    private String targetColumn;
    private RelationshipType relationshipType;
    private double confidence;
}
```
**Purpose:** Represents a relationship between two tables

#### RelationshipType.java
```java
public enum RelationshipType {
    STRICT,          // Foreign key constraints
    DATA_SAMPLE      // Inferred from data patterns
}
```

#### ForeignKey.java
**Purpose:** Additional metadata for foreign key information

### com.yogesh.er_scanner.service
**Business Logic**

#### SchemaScanner.java (152 lines)

**Injected Dependencies:**
- `DataSource` - JDBC connection pool
- `RelationshipConfig` - Configuration

**Main Method: `scan()`**

```
Process Flow:
1. Validate configuration is enabled
2. Get list of tables to scan
3. Validate table count doesn't exceed limit
4. For each configured table:
   a. Extract column metadata via DatabaseMetaData.getColumns()
   b. Identify primary keys via DatabaseMetaData.getPrimaryKeys()
   c. Detect foreign keys via DatabaseMetaData.getImportedKeys()
   d. Verify foreign keys reference only configured tables
   e. Create Table object with Column list
5. Return Schema with all tables and relationships
```

**Key Features:**
- Uses JDBC DatabaseMetaData API (low-level, vendor-agnostic)
- Strict validation of referenced tables
- Efficient single-connection scanning
- Only detects STRICT relationships

#### SchemaService.java

**Responsibilities:**
- Process scanned schema
- Generate Mermaid diagrams
- Convert to JSON
- Split into domain-based chunks
- Export outputs

**Key Methods:**
- `buildSchema(Schema)` - Process and cache schema
- `getSchema()` - Return cached schema
- `generateMermaid()` - Full ER diagram
- `generateMermaidChunks(int)` - Domain-split diagrams

#### DataSampleService.java

**Responsibilities:**
- Analyze data patterns in tables
- Detect DATA_SAMPLE relationships
- Calculate confidence scores
- Identify implicit relationships

**Sample-Based Detection:**
- Samples up to N rows per table (configured)
- Analyzes column value patterns
- Matches column values between tables
- Calculates confidence (0.0 - 1.0)

### com.yogesh.er_scanner.controller
**REST API Endpoints**

#### SchemaController.java (84 lines)

**Injected Dependencies:**
- `SchemaScanner` - For triggering scans
- `SchemaService` - For data retrieval

**Endpoints:**

1. **POST /schema/scan**
   - Triggers complete database scan
   - Calls `SchemaScanner.scan()`
   - Processes via `SchemaService.buildSchema()`
   - Returns: Success/error message

2. **GET /schema/json**
   - Returns complete schema as JSON
   - Contains all tables and relationships
   - AI-compatible format

3. **GET /schema/er-mermaid**
   - Returns full ER diagram in Mermaid syntax
   - Single diagram for all tables

4. **GET /schema/er-mermaid-domains**
   - Returns Map of domain-based diagrams
   - Splits large schemas for readability
   - Each domain gets separate diagram

### com.yogesh.er_scanner.db
**Database Utilities**
- Connection pooling helpers
- Database metadata utilities

### com.yogesh.er_scanner.dto
**Data Transfer Objects**
- Request/response models
- API contract definitions

### com.yogesh.er_scanner.enum
**Enumeration Types**
- RelationshipType (STRICT, DATA_SAMPLE)
- Status types
- Configuration enums

---

## 🔄 SCANNING PROCESS FLOW

```
Application Start
        │
        ▼
Spring Boot Initializes
        │
        ▼
DataSource Bean Created
(HikariCP Connection Pool)
        │
        ▼
RelationshipConfig Loaded
(from application.yaml)
        │
        ▼
Controllers Registered
        │
        ▼
Application Ready on Port 8080
        │
        ├─────────────────────────┐
        │                         │
    [POST /schema/scan]       [GET /schema/json]
        │                         │
        ▼                         ▼
    SchemaScanner.scan()    Return cached schema
        │
        ▼
    Connect to Database
        │
        ▼
    Load DatabaseMetaData
        │
        ├─► For each table:
        │   ├─► Extract columns
        │   ├─► Identify PKs
        │   └─► Detect FKs
        │
        ▼
    Build Schema Object
        │
        ▼
    SchemaService.buildSchema()
        │
        ├─► Merge relationships
        ├─► Generate Mermaid diagram
        ├─► Cache schema
        └─► Export JSON
        │
        ▼
    Return Success Response
```

---

## 🗄️ DATABASE CONNECTION DETAILS

**Connection Pool Configuration:**
```yaml
hikari:
  maximum-pool-size: 5          # Max concurrent connections
  minimum-idle: 1               # Minimum idle connections
  connection-timeout: 30000ms   # 30 second timeout
  idle-timeout: 600000ms        # 10 minute idle timeout
  max-lifetime: 1800000ms       # 30 minute max lifetime
```

**Database Target:**
- **URL:** `jdbc:mysql://localhost:3307/er_test`
- **Host:** localhost
- **Port:** 3307 (non-standard)
- **Database:** er_test
- **Username:** root
- **Password:** root

**JDBC Driver:** MySQL Connector/J (com.mysql.cj.jdbc.Driver)

**Connection Features:**
- SSL disabled for local development
- Public key retrieval enabled
- UTC timezone configured

---

## 📊 DATA FLOW: Scan Operation

```
┌──────────────────────────────────┐
│ 1. POST /schema/scan             │
│    Trigger complete scan         │
└──────────────┬───────────────────┘
               │
┌──────────────▼───────────────────┐
│ 2. SchemaScanner.scan()          │
│    Validate config               │
│    Get table list                │
└──────────────┬───────────────────┘
               │
┌──────────────▼───────────────────┐
│ 3. DatabaseMetaData API          │
│    ├─ getColumns()               │
│    ├─ getPrimaryKeys()           │
│    └─ getImportedKeys()          │
└──────────────┬───────────────────┘
               │
┌──────────────▼───────────────────┐
│ 4. Build Table Objects           │
│    For each column:              │
│    - Name, Type, PK, FK flags    │
└──────────────┬───────────────────┘
               │
┌──────────────▼───────────────────┐
│ 5. Build Relationship Objects    │
│    sourceTable → targetTable     │
│    RelationshipType.STRICT       │
│    Confidence: 1.0               │
└──────────────┬───────────────────┘
               │
┌──────────────▼───────────────────┐
│ 6. Return Schema Object          │
│    {tables, relationships}       │
└──────────────┬───────────────────┘
               │
┌──────────────▼───────────────────┐
│ 7. SchemaService.buildSchema()   │
│    Process & enhance             │
│    Generate diagrams             │
│    Cache in memory               │
└──────────────┬───────────────────┘
               │
┌──────────────▼───────────────────┐
│ 8. Export Outputs                │
│    ├─ schema-ai.json             │
│    ├─ er_diagram.mmd             │
│    └─ ENTERPRISEDOMAINS.json     │
└──────────────────────────────────┘
```

---

## 🔐 SECURITY CONSIDERATIONS

**Current State (Development):**
- ✓ Hardcoded credentials in application.yaml
- ✓ No SSL verification
- ✓ No authentication on REST endpoints
- ✓ No rate limiting

**Recommended for Production:**
- [ ] Move credentials to environment variables
- [ ] Enable SSL/TLS
- [ ] Implement OAuth2/JWT authentication
- [ ] Add rate limiting
- [ ] Implement input validation
- [ ] Add audit logging
- [ ] Use separate read-only database user

---

## 📈 PERFORMANCE CHARACTERISTICS

**Scalability:**
- **Connection Pool:** 5 concurrent connections (configurable)
- **Max Tables:** 100 (configurable)
- **Sample Size:** 20 rows per table (configurable)
- **Memory:** In-memory caching of schema

**Time Complexity:**
- Scanning: O(T × C) where T = tables, C = columns
- Relationship detection: O(R) where R = relationships
- Diagram generation: O(T × E) where E = edges

**Space Complexity:**
- O(T + C + R) for schema storage

---

## 🧪 TESTING

**Test Location:** `src/test/java/com/yogesh/er_scanner/`

**Test Classes:**
- `ErScannerApplicationTests.java` - Integration tests

**Testing Approach:**
- Spring Boot Test framework
- In-memory database testing (H2)
- Mock objects for external dependencies

---

## 🔧 BUILD CONFIGURATION

**Maven Plugins:**
- spring-boot-maven-plugin (4.0.3)
- maven-compiler-plugin (Java 17)
- maven-surefire-plugin (test execution)

**Dependencies Summary:**
- Spring Boot Starter (Core)
- Spring Boot Starter Web (REST)
- Spring Boot Starter JDBC (Database)
- MySQL Connector/J (Driver)
- Additional utilities as needed

---

## 📝 CONFIGURATION INJECTION

**ApplicationYAML Binding:**
```
application.yaml
      │
      ▼
Spring ConfigurationProperties
      │
      ▼
RelationshipConfig Bean
      │
      ▼
SchemaScanner Service
```

**Configuration is injected** via Spring's `@ConfigurationProperties` annotation, binding YAML properties to Java objects.

---

## 🎯 FUTURE ENHANCEMENTS

1. **Database Polymorphism**
   - Support for PostgreSQL, Oracle, SQL Server
   - Vendor-specific optimizations

2. **Advanced Relationship Detection**
   - Machine learning-based pattern recognition
   - Multi-column relationship detection
   - Temporal relationship analysis

3. **Interactive UI**
   - Web dashboard for visualization
   - Real-time diagram editing
   - Relationship strength indicators

4. **Comparative Analysis**
   - Schema versioning
   - Diff generation between versions
   - Change impact analysis

5. **Performance Optimization**
   - Parallel scanning
   - Incremental updates
   - Caching strategies

---

## 📚 DEPENDENCIES ANALYSIS

### Runtime Dependencies
- **Spring Boot Starter:** Core framework
- **Spring Boot Starter Web:** REST API support
- **Spring Boot Starter JDBC:** Database connectivity
- **MySQL Connector/J:** MySQL driver
- **HikariCP:** Connection pooling (embedded)

### Build Dependencies
- **Maven:** Build and dependency management
- **Java 17:** Language runtime

### Optional (can be added)
- **Lombok:** Reduce boilerplate
- **JUnit 5:** Enhanced testing
- **Testcontainers:** Docker-based testing

---

## 🏁 CONCLUSION

The ER Scanner is a well-structured, layered Spring Boot application designed for:
- ✅ Automated schema discovery
- ✅ Relationship detection (STRICT & DATA_SAMPLE)
- ✅ Business domain classification
- ✅ Visual diagram generation (Mermaid)
- ✅ JSON export for AI/ML processing

**Code Quality:** Modular, testable, maintainable
**Architecture:** Clean layered design with clear separation of concerns
**Extensibility:** Easy to add new relationship detection algorithms
**Performance:** Efficient JDBC-based scanning with connection pooling


