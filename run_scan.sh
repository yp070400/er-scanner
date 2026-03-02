#!/usr/bin/env bash
set -euo pipefail

# Usage: ./run_scan.sh
# Ensure DB_URL, DB_USER, DB_PASS env vars are set if not using defaults

DB_URL=${DB_URL:-"jdbc:mysql://localhost:3307/er_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"}
DB_USER=${DB_USER:-root}
DB_PASS=${DB_PASS:-root}

echo "Building project..."
mvn clean package -DskipTests

JAR=target/er-scanner-0.0.1-SNAPSHOT.jar
if [ ! -f "$JAR" ]; then
  echo "Jar not found: $JAR"
  exit 1
fi

LOGFILE=run_live.log

echo "Starting application (logs -> $LOGFILE)"
DB_URL="$DB_URL" DB_USER="$DB_USER" DB_PASS="$DB_PASS" nohup java -jar "$JAR" > "$LOGFILE" 2>&1 &
APP_PID=$!

echo "Waiting for app to start (10s)..."
sleep 10

echo "Triggering schema scan..."
HTTP=$(curl -s -o /tmp/scan_response.txt -w "%{http_code}" -X POST http://localhost:8080/schema/scan || true)
echo "Scan request HTTP status: $HTTP"

if [ "$HTTP" != "200" ]; then
  echo "Scan may have failed. Showing last 200 lines of log:"
  tail -n 200 "$LOGFILE"
  exit 1
fi

echo "Fetching schema JSON..."
curl -s http://localhost:8080/schema/json -o output/schema-from-server.json

echo "Fetching mermaid ER..."
curl -s http://localhost:8080/schema/er-mermaid -o output/er_diagram-from-server.mmd

echo "Fetching domain mermaid chunks..."
curl -s http://localhost:8080/schema/er-mermaid-domains -o output/er_domains.json

echo "Done. Outputs in output/ directory."

echo "PID of app: $APP_PID"

echo "To stop the app: kill $APP_PID"

