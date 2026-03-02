#!/bin/bash

# Script to run scan and ensure output files are generated

cd /Users/yogeshchandraprasad/Development/Projects/er-scanner

echo "=========================================="
echo "ER Scanner - Output File Generation Test"
echo "=========================================="
echo ""

# Kill any existing processes
lsof -ti:8080 | xargs kill -9 2>/dev/null || true
sleep 2

# Clean output directory (except domains.json which is needed)
rm -f output/er_diagram-from-server.mmd
rm -f output/er_domains_new.json
rm -f output/schema-ai-new.json

echo "✓ Cleaned old output files"
echo ""

# Start the application
echo "Starting application..."
DB_URL='jdbc:mysql://localhost:3307/er_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' \
DB_USER='root' \
DB_PASS='root' \
java -jar target/er-scanner-0.0.1-SNAPSHOT.jar > app.log 2>&1 &

APP_PID=$!
echo "✓ Application started (PID: $APP_PID)"
echo ""

# Wait for app to start
echo "Waiting for application to start..."
sleep 8

# Trigger the scan
echo "Triggering schema scan..."
HTTP_CODE=$(curl -s -w "%{http_code}" -o /tmp/scan_response.txt -X POST http://localhost:8080/schema/scan)
echo "✓ Scan request HTTP status: $HTTP_CODE"
echo ""

# Wait for processing
sleep 3

# Check which files were generated
echo "=========================================="
echo "Output Files Generated:"
echo "=========================================="
ls -lh output/schema-ai.json 2>/dev/null && echo "✓ schema-ai.json" || echo "✗ schema-ai.json NOT FOUND"
ls -lh output/er_diagram-from-server.mmd 2>/dev/null && echo "✓ er_diagram-from-server.mmd" || echo "✗ er_diagram-from-server.mmd NOT FOUND"
ls -lh output/er_domains.json 2>/dev/null && echo "✓ er_domains.json" || echo "✗ er_domains.json NOT FOUND"
echo ""

# Show app logs
echo "=========================================="
echo "Application Logs (filtered):"
echo "=========================================="
grep -E "✓|⚠|ERROR|SchemaService|Writing" app.log | tail -n 30 || echo "No matching logs found"
echo ""

# Show last 50 lines of app log if no filtered results
if ! grep -q "Writing" app.log; then
    echo "Last 50 lines of app.log:"
    tail -n 50 app.log
fi

echo ""
echo "=========================================="
echo "Stopping application (PID: $APP_PID)"
echo "=========================================="
kill $APP_PID 2>/dev/null || true
sleep 2

echo "Test complete."

