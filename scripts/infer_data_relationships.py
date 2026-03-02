#!/usr/bin/env python3
import os
import re
import json
import yaml
import pymysql
from urllib.parse import urlparse, parse_qs

# Configuration
PROJECT_ROOT = os.path.dirname(os.path.dirname(__file__))
APP_YAML = os.path.join(PROJECT_ROOT, 'src', 'main', 'resources', 'application.yaml')
SCHEMA_AI = os.path.join(PROJECT_ROOT, 'output', 'schema-ai.json')
OUT_MERGED = os.path.join(PROJECT_ROOT, 'output', 'schema-ai-merged.json')

# Helpers
def parse_jdbc_url(jdbc_url):
    # Example: jdbc:mysql://localhost:3307/er_test?useSSL=false&serverTimezone=UTC
    m = re.match(r'jdbc:mysql://([^/]+)/(\w+)(?:\?(.*))?', jdbc_url)
    if not m:
        raise ValueError(f"Invalid JDBC URL: {jdbc_url}")
    hostport = m.group(1)
    db = m.group(2)
    params = m.group(3) or ''
    if ':' in hostport:
        host, port = hostport.split(':', 1)
        port = int(port)
    else:
        host = hostport
        port = 3306
    return host, port, db, params


def load_config():
    cfg = {}
    if os.path.exists(APP_YAML):
        with open(APP_YAML, 'r') as f:
            y = yaml.safe_load(f)
            # datasource
            ds = y.get('spring', {}).get('datasource', {})
            cfg['db_url'] = os.environ.get('DB_URL') or ds.get('url')
            cfg['db_user'] = os.environ.get('DB_USER') or ds.get('username')
            cfg['db_pass'] = os.environ.get('DB_PASS') or ds.get('password')
            rel = y.get('relationship', {})
            cfg['sample_size'] = rel.get('sample-size', 20)
            cfg['tables'] = rel.get('tables', [])
    else:
        # Fallback to env
        cfg['db_url'] = os.environ.get('DB_URL')
        cfg['db_user'] = os.environ.get('DB_USER')
        cfg['db_pass'] = os.environ.get('DB_PASS')
        cfg['sample_size'] = int(os.environ.get('SAMPLE_SIZE', '20'))
        cfg['tables'] = os.environ.get('TABLES', '').split(',') if os.environ.get('TABLES') else []
    return cfg


def sample_table_columns(conn, table, sample_size):
    # return dict: column -> set(values)
    out = {}
    with conn.cursor() as cur:
        cur.execute(f"SELECT * FROM `{table}` LIMIT {sample_size}")
        rows = cur.fetchall()
        if not rows:
            return {}
        # get column names
        cols = [d[0] for d in cur.description]
        for i,col in enumerate(cols):
            values = set()
            for row in rows:
                val = row[i]
                if val is None:
                    continue
                values.add(str(val))
                if len(values) > 2000:
                    # safety
                    break
            if values:
                out[col] = values
    return out


def compute_overlaps(sampled):
    rels = []
    tables = list(sampled.keys())
    for i in range(len(tables)):
        for j in range(i+1, len(tables)):
            t1 = tables[i]
            t2 = tables[j]
            c1 = sampled[t1]
            c2 = sampled[t2]
            for col1, s1 in c1.items():
                for col2, s2 in c2.items():
                    if not s1 or not s2:
                        continue
                    inter = s1.intersection(s2)
                    overlap = len(inter) / min(len(s1), len(s2))
                    if overlap >= 0.6:
                        rel_type = 'DATA_INFERRED' if overlap >= 0.9 else 'DATA_SAMPLE'
                        rels.append({
                            'sourceTable': t1,
                            'sourceColumn': col1,
                            'targetTable': t2,
                            'targetColumn': col2,
                            'relationshipType': rel_type,
                            'confidence': round(overlap, 4)
                        })
    return rels


def merge_into_schema_ai(existing_path, new_rels):
    if not os.path.exists(existing_path):
        schema = {'tables': [], 'relationships': []}
    else:
        with open(existing_path, 'r') as f:
            schema = json.load(f)
    existing = schema.get('relationships', [])
    # build key set to avoid duplicates
    keys = set()
    for r in existing:
        keys.add((r.get('sourceTable'), r.get('sourceColumn'), r.get('targetTable'), r.get('targetColumn'), r.get('relationshipType')))
    added = 0
    for nr in new_rels:
        key = (nr['sourceTable'], nr['sourceColumn'], nr['targetTable'], nr['targetColumn'], nr['relationshipType'])
        if key in keys:
            continue
        existing.append(nr)
        keys.add(key)
        added +=1
    schema['relationships'] = existing
    with open(OUT_MERGED, 'w') as f:
        json.dump(schema, f, indent=2)
    return added


def main():
    cfg = load_config()
    if not cfg.get('db_url'):
        print('DB URL not found in application.yaml or env (DB_URL). Aborting.')
        return
    host, port, dbname, params = parse_jdbc_url(cfg['db_url'])
    user = cfg.get('db_user') or os.environ.get('DB_USER')
    pwd = cfg.get('db_pass') or os.environ.get('DB_PASS')
    sample_size = int(cfg.get('sample_size', 20))
    tables = cfg.get('tables') or []
    if not tables:
        print('No tables configured for sampling. Aborting.')
        return

    print(f'Connecting to {host}:{port}/{dbname} as {user}, sampling {len(tables)} tables, sample_size={sample_size}')

    conn = pymysql.connect(host=host, port=port, user=user, password=pwd, database=dbname, cursorclass=pymysql.cursors.Cursor)
    sampled = {}
    for t in tables:
        t = t.strip()
        if not t:
            continue
        try:
            vals = sample_table_columns(conn, t, sample_size)
            sampled[t] = vals
            print(f'  sampled {t}: {len(vals)} columns')
        except Exception as e:
            print(f'  failed to sample {t}: {e}')
    conn.close()

    new_rels = compute_overlaps(sampled)
    print(f'Detected {len(new_rels)} inferred/sample relationships')
    added = merge_into_schema_ai(SCHEMA_AI, new_rels)
    print(f'Added {added} new relationships to {OUT_MERGED}')

if __name__ == '__main__':
    main()

