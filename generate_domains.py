#!/usr/bin/env python3
import json
import sys

# Read schema
try:
    with open('output/schema-ai.json', 'r') as f:
        schema = json.load(f)
except Exception as e:
    print(f"Error reading schema: {e}", file=sys.stderr)
    sys.exit(1)

# Get all table names
all_table_names = {table['name'] for table in schema['tables']}

# Group tables by domain based on relationships and naming
domains = {}

# 1. CORE ORDER MANAGEMENT DOMAIN
# Orders, order_items, payments, invoices, shipments connected via order_id
order_mgmt = ['orders', 'order_items', 'payments', 'invoices', 'shipments']
domains['Order Management'] = order_mgmt

# 2. CUSTOMER & USER MANAGEMENT DOMAIN
# customers, users, tenants, roles, user_roles, notifications
user_mgmt = ['customers', 'users', 'tenants', 'roles', 'user_roles', 'notifications']
domains['Customer & User Management'] = user_mgmt

# 3. PRODUCT CATALOG DOMAIN
# products, categories, inventory
product_domain = ['products', 'categories', 'inventory']
domains['Product Catalog & Inventory'] = product_domain

# 4. AUDIT & OPERATIONAL DOMAIN
# audit_logs, orphan_table (unrelated)
operational = ['audit_logs', 'orphan_table']
domains['Audit & Operational'] = operational

# 5. AUXILIARY/GENERIC TABLES DOMAIN
# aux_table_1 through aux_table_50 - these are generic tables that reference orders/users/tenants
aux_tables = [f'aux_table_{i}' for i in range(1, 51)]
domains['Auxiliary Services'] = aux_tables

# Verify all tables are accounted for
all_classified = set()
for domain_tables in domains.values():
    all_classified.update(domain_tables)

missing = all_table_names - all_classified
if missing:
    domains['Miscellaneous'] = list(missing)

# Output JSON - ONLY valid JSON, no explanations
result = {}
for domain_name in sorted(domains.keys()):
    result[domain_name] = sorted(domains[domain_name])

print(json.dumps(result, indent=2))

