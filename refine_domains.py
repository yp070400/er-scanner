#!/usr/bin/env python3
import json
from collections import defaultdict, deque

# Read schema
with open('output/schema-ai.json', 'r') as f:
    schema = json.load(f)

# Build relationship graph using STRICT relationships
graph = defaultdict(set)

for rel in schema['relationships']:
    if rel['relationshipType'] == 'STRICT':
        source = rel['sourceTable']
        target = rel['targetTable']
        graph[source].add(target)
        graph[target].add(source)

# Get all tables
all_tables = {table['name'] for table in schema['tables']}

# Find connected components using BFS
visited = set()
components = []

def bfs_component(start):
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

# Classify and group tables by business domain
domains = {}

# Define table categories
order_related = {'orders', 'order_items', 'invoices', 'shipments', 'payments'}
user_related = {'users', 'tenants', 'roles', 'user_roles', 'notifications', 'audit_logs'}
product_related = {'products', 'categories', 'inventory'}
aux_related = {t for t in all_tables if t.startswith('aux_table_')}

for component in components:
    business_tables = [t for t in component if not t.startswith('aux_table_')]
    aux_tables = [t for t in component if t.startswith('aux_table_')]

    if business_tables:
        # Classify based on primary business function
        order_tables = [t for t in business_tables if t in order_related]
        user_tables = [t for t in business_tables if t in user_related]
        product_tables = [t for t in business_tables if t in product_related]

        # Assign to appropriate domain
        if order_tables:
            # Orders connected to users and products
            domains['Order Management'] = sorted(order_tables)

        if user_tables:
            # User management including audit
            domains['Customer & User Management'] = sorted(user_tables)

        if product_tables:
            # Product catalog
            domains['Product Catalog & Inventory'] = sorted(product_tables)

    # Auxiliary tables go to their own domain
    if aux_tables:
        if 'Auxiliary Services' not in domains:
            domains['Auxiliary Services'] = []
        domains['Auxiliary Services'].extend(aux_tables)

# Ensure all tables are classified
classified = set()
for domain_tables in domains.values():
    classified.update(domain_tables)

# Double-check all aux tables are included
aux_all = {t for t in all_tables if t.startswith('aux_table_')}
if 'Auxiliary Services' not in domains:
    domains['Auxiliary Services'] = []
domains['Auxiliary Services'] = sorted(list(aux_all))

# Final output
result = {}
for domain_name in sorted(domains.keys()):
    result[domain_name] = sorted(domains[domain_name])

print(json.dumps(result, indent=2))

