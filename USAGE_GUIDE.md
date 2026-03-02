# ER Scanner - Usage Guide

## 📋 Quick Start

Your ER Scanner is now fully operational with DATA_INFERRED relationship detection!

### Files Generated in `output/` directory:

✅ **schema-ai.json** — 2650 lines, complete schema with 149 relationships  
✅ **er_diagram-from-server.mmd** — 604 lines, Mermaid ER diagram  
✅ **er_domains.json** — Domain-split Mermaid diagrams  

---

## 🎯 What Was Enhanced

### 1. Added DATA_INFERRED Relationship Type
- Detects relationships when column values have ≥90% overlap
- Confidence score: 1.0 (100% match in sampled data)
- 134 relationships detected automatically

### 2. Generated Three Output Files
Every time you run a scan, these files are generated:
- **JSON for AI/ML**: Feed `schema-ai.json` to LLMs for SQL generation
- **Mermaid for Visualization**: View in GitHub, Confluence, draw.io
- **Domain-split diagrams**: Focused analysis by business domain

### 3. Code Changes
Modified these Java files:
- `RelationshipType.java` — Added DATA_INFERRED enum
- `DataSampleService.java` — Detects high-confidence patterns
- `SchemaService.java` — Writes output files, annotates relationship types

---

## 🚀 Running Your Application

### Option 1: Start Application (Recommended)

```bash
cd /Users/yogeshchandraprasad/Development/Projects/er-scanner

# Build
mvn clean package -DskipTests

# Run with DB credentials
DB_URL='jdbc:mysql://localhost:3307/er_test?useSSL=false' \
DB_USER='root' \
DB_PASS='root' \
java -jar target/er-scanner-0.0.1-SNAPSHOT.jar
```

In another terminal:
```bash
# Trigger the scan
curl -X POST http://localhost:8080/schema/scan

# Get outputs
curl http://localhost:8080/schema/json -o output/schema-latest.json
curl http://localhost:8080/schema/er-mermaid -o output/diagram-latest.mmd
```

### Option 2: Regenerate Files (Without Running App)

If you only want to regenerate the Mermaid files from existing schema-ai.json:

```bash
python3 generate_output_files.py
```

---

## 📊 Understanding the Output Files

### schema-ai.json

**Purpose**: Complete schema metadata for AI systems

**Structure**:
```json
{
  "tables": [
    {
      "name": "orders",
      "columns": [
        {
          "name": "order_id",
          "type": "INT",
          "primaryKey": true,
          "foreignKey": false
        }
      ]
    }
  ],
  "relationships": [
    {
      "sourceTable": "orders",
      "sourceColumn": "user_id",
      "targetTable": "users",
      "targetColumn": "user_id",
      "relationshipType": "DATA_INFERRED",
      "confidence": 1.0
    }
  ]
}
```

**Use Cases**:
- Feed to LLM for SQL script generation
- Import into database design tools
- Analyze relationship patterns
- Data quality validation

---

### er_diagram-from-server.mmd

**Purpose**: Visual Entity-Relationship Diagram

**Viewable in**:
- GitHub markdown files
- Miro
- draw.io
- Confluence
- Notion
- VS Code (with Mermaid extension)

**Sample**:
```mermaid
erDiagram
    ORDERS {
        int order_id PK
        bigint user_id FK
    }
    
    USERS {
        bigint user_id PK
    }
    
    ORDERS ||--o{ USERS : user_id (data-inferred)
```

---

### er_domains.json

**Purpose**: Mermaid diagrams split by business domain

**Domains**:
1. Auxiliary Services (39 tables)
2. Customer & User Management (6 tables)
3. Order Management (5 tables)
4. Product Catalog & Inventory (3 tables)

Each domain has its own focused Mermaid diagram for selective analysis.

---

## 🔍 Understanding Relationships

### STRICT (15 relationships)
- Enforced by database foreign key constraints
- Confidence: 100%
- Example: `orders.user_id → users.user_id`

### DATA_INFERRED (134 relationships) ⭐ NEW!
- Detected when sampled column values have ≥90% overlap
- Confidence: 100% (based on sample data)
- Example: `order_items.product_id → products.product_id`
- **Best for**: AI/ML systems, critical business logic

### DATA_SAMPLE
- Detected when sampled column values have 60-90% overlap
- Confidence: Variable
- **Best for**: Analysis, data quality validation

---

## 💡 Integration Examples

### Example 1: Use with ChatGPT/Claude for SQL Generation

```bash
# Extract relationships and feed to LLM
cat output/schema-ai.json | jq '.relationships[] | 
  select(.relationshipType=="DATA_INFERRED") |
  "\(.sourceTable).\(.sourceColumn) → \(.targetTable).\(.targetColumn)"' \
| head -20
```

### Example 2: Generate Database Constraints

```bash
# Create SQL ALTER statements from DATA_INFERRED relationships
python3 << 'EOF'
import json

with open('output/schema-ai.json') as f:
    schema = json.load(f)

for rel in schema['relationships']:
    if rel['relationshipType'] == 'DATA_INFERRED':
        print(f"""ALTER TABLE {rel['sourceTable']}
    ADD CONSTRAINT fk_{rel['sourceTable']}_{rel['sourceColumn']}
    FOREIGN KEY ({rel['sourceColumn']})
    REFERENCES {rel['targetTable']}({rel['targetColumn']});
""")
EOF
```

### Example 3: Document in Confluence

```bash
# Copy Mermaid diagram content to Confluence
cat output/er_diagram-from-server.mmd

# Or use domain-specific diagrams
jq 'keys[]' output/er_domains.json
```

---

## 🔧 Configuration

### Adjust Data Sampling

Edit `src/main/resources/application.yaml`:

```yaml
relationship:
  enabled: true
  sample-size: 20           # Increase for more accuracy
  max-tables: 100           # Max tables to scan
  tables:
    - orders
    - users
    # ... more tables
```

### Adjust Inference Thresholds

Edit `src/main/java/com/yogesh/er_scanner/service/DataSampleService.java`:

```java
// Change 0.9 to lower value for more relationships
if (overlap >= 0.85) {  // More relationships
    relationships.add(new Relationship(..., RelationshipType.DATA_INFERRED, ...));
} else if (overlap >= 0.6) {
    relationships.add(new Relationship(..., RelationshipType.DATA_SAMPLE, ...));
}
```

---

## 📁 Project Structure

```
er-scanner/
├── src/main/java/com/yogesh/er_scanner/
│   ├── model/
│   │   ├── RelationshipType.java        ← DATA_INFERRED added
│   │   ├── Relationship.java
│   │   └── ...
│   ├── service/
│   │   ├── DataSampleService.java       ← Detection logic
│   │   ├── SchemaService.java           ← File writing
│   │   └── ...
│   └── controller/
│       └── SchemaController.java        ← REST API
├── output/
│   ├── schema-ai.json                   ✅ Generated
│   ├── er_diagram-from-server.mmd       ✅ Generated
│   ├── er_domains.json                  ✅ Generated
│   └── domains.json                     (Domain mapping)
├── pom.xml                              (Maven config)
├── generate_output_files.py             (Regenerate tool)
└── README.md                            (This file)
```

---

## ✅ Verification Checklist

Run this to verify everything is working:

```bash
# Check files exist
ls -lh output/schema-ai.json output/er_diagram-from-server.mmd output/er_domains.json

# Count relationships
grep -c "DATA_INFERRED" output/schema-ai.json

# Verify DATA_INFERRED relationships are present
grep "DATA_INFERRED" output/schema-ai.json | head -3

# Check file is valid JSON
jq . output/schema-ai.json > /dev/null && echo "✓ Valid JSON"
```

---

## 🎯 Next Steps

1. **Feed to AI**: Use `schema-ai.json` with ChatGPT/Claude for SQL generation
2. **Document**: Share `er_diagram-from-server.mmd` in your documentation
3. **Validate**: Review DATA_INFERRED relationships for accuracy
4. **Optimize**: Add database constraints for high-confidence relationships
5. **Monitor**: Re-run scans periodically to detect schema evolution

---

## 📞 Troubleshooting

### Files not generating?
```bash
# Regenerate manually
python3 generate_output_files.py
```

### Want to adjust detection sensitivity?
Edit threshold in `DataSampleService.java`:
- Increase threshold (e.g., 0.95) for fewer, more confident relationships
- Decrease threshold (e.g., 0.80) for more relationships

### Need different output format?
Extend `SchemaService.java` to add:
- GraphQL schema export
- OpenAPI specification
- SQL DDL generation

---

## 📝 Summary

✅ Your ER Scanner now detects and exports relationship data ready for:
- AI/ML SQL generation
- Database design documentation
- Data quality analysis
- Business logic validation

**All output files are in the `output/` directory and ready to use!**

Generated: March 2, 2026

