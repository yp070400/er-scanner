#!/usr/bin/env python3
import json
from collections import defaultdict, deque

# Read schema
with open('output/schema-ai.json', 'r') as f:
    schema = json.load(f)

# Build relationship graph using STRICT relationships only
graph = defaultdict(set)

for rel in schema['relationships']:
    # Only use STRICT relationships for domain grouping
    if rel['relationshipType'] == 'STRICT':
        source = rel['sourceTable']
        target = rel['targetTable']
        graph[source].add(target)
        graph[target].add(source)

# Get all tables
all_tables = {table['name'] for table in schema['tables']}

# Find connected components using BFS - these form the base domains
visited = set()
components = []

def bfs_component(start):
    """Find all tables connected to start table via STRICT relationships"""
    component = set()
    queue = deque([start])
    visited.add(start)

    while queue:
        node = queue.popleft()
        component.add(node)
        for neighbor in graph[node]:
            if neighbor not in visited:
                visited.add(neighbor)
                queue.append(neighbor)

    return component

# Find all connected components
for table in sorted(all_tables):
    if table not in visited:
        component = bfs_component(table)
        components.append(sorted(component))

# Classify tables by business domain
domains = {}

for component in components:
    # Separate aux tables from business tables
    aux_tables = [t for t in component if t.startswith('aux_table_')]
    business_tables = [t for t in component if not t.startswith('aux_table_')]

    if business_tables:
        # Determine domain based on core business tables present
        domain_name = None

        if 'orders' in business_tables:
            domain_name = 'Order Management'
        elif 'users' in business_tables and 'tenants' in business_tables:
            domain_name = 'Customer & User Management'
        elif 'products' in business_tables or 'categories' in business_tables:
            domain_name = 'Product Catalog & Inventory'
        elif 'audit_logs' in business_tables:
            domain_name = 'Audit & Operations'
        else:
            domain_name = 'Miscellaneous'

        if domain_name not in domains:
            domains[domain_name] = []

        domains[domain_name].extend(business_tables)

        # Add aux tables to the domain if they relate to this component
        if aux_tables:
            domains[domain_name].extend(aux_tables)
    else:
        # All aux tables - group them together
        if 'Auxiliary Services' not in domains:
            domains['Auxiliary Services'] = []
        domains['Auxiliary Services'].extend(aux_tables)

# Verify all tables are classified
classified = set()
for domain_tables in domains.values():
    classified.update(domain_tables)

missing = all_tables - classified
if missing:
    domains['Miscellaneous'] = sorted(list(missing))

# Sort and output
result = {}
for domain_name in sorted(domains.keys()):
    result[domain_name] = sorted(domains[domain_name])

print(json.dumps(result, indent=2))

