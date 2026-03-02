#!/usr/bin/env python3
import json
import os

# Read the schema-ai.json
with open('output/schema-ai.json', 'r') as f:
    schema = json.load(f)

tables = schema.get('tables', [])
relationships = schema.get('relationships', [])

# ===== GENERATE MERMAID DIAGRAM =====
mermaid = "erDiagram\n    direction TB\n\n"

# Add tables
for table in tables:
    mermaid += f"    {table['name'].upper()} {{\n"
    for col in table['columns']:
        col_def = f"        string {col['name']}"
        if col.get('primaryKey'):
            col_def += " PK"
        elif col.get('foreignKey'):
            col_def += " FK"
        mermaid += col_def + "\n"
    mermaid += "    }\n\n"

# Add relationships
for rel in relationships:
    label = rel['sourceColumn']
    rel_type = rel.get('relationshipType', 'STRICT')

    # Add type annotation
    if rel_type == 'DATA_INFERRED':
        label += " (data-inferred)"
    elif rel_type == 'DATA_SAMPLE':
        label += " (data-sample)"
    elif rel_type == 'INFERRED':
        label += " (inferred)"

    mermaid += f"    {rel['sourceTable'].upper()} ||--o{{ {rel['targetTable'].upper()} : {label}\n"

# Write Mermaid file
with open('output/er_diagram-from-server.mmd', 'w') as f:
    f.write(mermaid)
print("✓ Generated: output/er_diagram-from-server.mmd")

# ===== GENERATE DOMAINS MERMAID (optional, from ENTERPRISEDOMAINS.json) =====
if os.path.exists('output/ENTERPRISEDOMAINS.json'):
    with open('output/ENTERPRISEDOMAINS.json', 'r') as f:
        domains_map = json.load(f)

    domains_mermaid = {}

    for domain_name, domain_tables in domains_map.items():
        domain_rels = [r for r in relationships
                      if r['sourceTable'] in domain_tables and r['targetTable'] in domain_tables]

        domain_tables_set = {t for t in domain_tables if any(
            tbl['name'] == t for tbl in tables
        )}

        if domain_tables_set and domain_rels:
            mmd = f"erDiagram\n    direction TB\n\n"

            for table in tables:
                if table['name'] in domain_tables_set:
                    mmd += f"    {table['name'].upper()} {{\n"
                    for col in table['columns']:
                        if col.get('primaryKey') or col.get('foreignKey'):
                            col_def = f"        string {col['name']}"
                            if col.get('primaryKey'):
                                col_def += " PK"
                            elif col.get('foreignKey'):
                                col_def += " FK"
                            mmd += col_def + "\n"
                    mmd += "    }\n\n"

            for rel in domain_rels:
                label = rel['sourceColumn']
                rel_type = rel.get('relationshipType', 'STRICT')
                if rel_type == 'DATA_INFERRED':
                    label += " (data-inferred)"
                elif rel_type == 'DATA_SAMPLE':
                    label += " (data-sample)"

                mmd += f"    {rel['sourceTable'].upper()} ||--o{{ {rel['targetTable'].upper()} : {label}\n"

            domains_mermaid[domain_name] = mmd

    with open('output/er_domains.json', 'w') as f:
        json.dump(domains_mermaid, f, indent=2)
    print(f"✓ Generated: output/er_domains.json ({len(domains_mermaid)} domains)")
else:
    print("⚠ ENTERPRISEDOMAINS.json not found, skipping domains file")

print("\n✓ All output files generated successfully!")

