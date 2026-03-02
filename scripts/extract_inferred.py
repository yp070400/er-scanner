#!/usr/bin/env python3
import json
import os

SCHEMA_AI = os.path.join(os.path.dirname(os.path.dirname(__file__)), 'output', 'schema-ai.json')

if not os.path.exists(SCHEMA_AI):
    print('schema-ai.json not found at', SCHEMA_AI)
    exit(1)

with open(SCHEMA_AI, 'r') as f:
    schema = json.load(f)

rels = schema.get('relationships', [])

inferred = [r for r in rels if r.get('relationshipType') == 'DATA_INFERRED']

print(f'Total relationships: {len(rels)}')
print(f'DATA_INFERRED relationships: {len(inferred)}\n')

for r in inferred:
    print(f"{r['sourceTable']}.{r['sourceColumn']} -> {r['targetTable']}.{r['targetColumn']} (confidence={r.get('confidence')})")

