#!/bin/bash
# Quick Start Guide - Run this to get all output files!

set -e

PROJECT_DIR="/Users/yogeshchandraprasad/Development/Projects/er-scanner"
cd "$PROJECT_DIR"

echo "=============================================="
echo "ER Scanner - Complete Setup & Run Guide"
echo "=============================================="
echo ""

# Step 1: Build
echo "Step 1: Building project..."
mvn clean package -DskipTests -q
echo "✓ Build complete"
echo ""

# Step 2: Kill any existing processes
echo "Step 2: Cleaning up old processes..."
pkill -9 -f "er-scanner" || true
sleep 2
echo "✓ Cleaned up"
echo ""

# Step 3: Remove old output files
echo "Step 3: Preparing output directory..."
rm -f output/er_diagram-from-server.mmd output/schema-from-server.json 2>/dev/null || true
echo "✓ Ready"
echo ""

# Step 4: Start the application
echo "Step 4: Starting ER Scanner application..."
DB_URL='jdbc:mysql://localhost:3307/er_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' \
DB_USER='root' \
DB_PASS='root' \
nohup java -jar target/er-scanner-0.0.1-SNAPSHOT.jar > app.log 2>&1 &

APP_PID=$!
echo "✓ Application started (PID: $APP_PID)"
sleep 5
echo ""

# Step 5: Trigger scan
echo "Step 5: Triggering schema scan..."
HTTP_CODE=$(curl -s -w "%{http_code}" -o /tmp/scan_response.txt -X POST http://localhost:8080/schema/scan)
echo "HTTP Status: $HTTP_CODE"

if [ "$HTTP_CODE" == "200" ]; then
    echo "✓ Scan successful!"
else
    echo "✗ Scan failed"
    echo "Response: $(cat /tmp/scan_response.txt)"
fi
echo ""

# Step 6: Wait and collect outputs
echo "Step 6: Collecting generated files..."
sleep 2

# Fetch files from API
curl -s http://localhost:8080/schema/json -o output/schema-from-server.json
curl -s http://localhost:8080/schema/er-mermaid -o output/er_diagram-from-server.mmd
curl -s http://localhost:8080/schema/er-mermaid-domains -o output/er_domains.json 2>/dev/null || true

echo "✓ Files collected"
echo ""

# Step 7: Verify files
echo "Step 7: Verifying output files..."
echo ""
echo "Generated files:"
ls -lh output/schema-ai.json 2>/dev/null && echo "  ✓ schema-ai.json" || echo "  ✗ schema-ai.json"
ls -lh output/schema-from-server.json 2>/dev/null && echo "  ✓ schema-from-server.json" || echo "  ✗ schema-from-server.json"
ls -lh output/er_diagram-from-server.mmd 2>/dev/null && echo "  ✓ er_diagram-from-server.mmd" || echo "  ✗ er_diagram-from-server.mmd"
ls -lh output/er_domains.json 2>/dev/null && echo "  ✓ er_domains.json" || echo "  ✗ er_domains.json"
echo ""

# Step 8: Show statistics
echo "Step 8: Relationship Statistics"
echo ""

if [ -f output/schema-ai.json ]; then
    STRICT_COUNT=$(grep -c '"relationshipType" : "STRICT"' output/schema-ai.json || echo "0")
    INFERRED_COUNT=$(grep -c '"relationshipType" : "DATA_INFERRED"' output/schema-ai.json || echo "0")
    TOTAL=$((STRICT_COUNT + INFERRED_COUNT))

    echo "  Total relationships: $TOTAL"
    echo "    - STRICT: $STRICT_COUNT"
    echo "    - DATA_INFERRED: $INFERRED_COUNT"
    echo ""

    echo "Sample DATA_INFERRED relationships:"
    grep -A2 '"relationshipType" : "DATA_INFERRED"' output/schema-ai.json | head -10
fi
echo ""

# Step 9: Done
echo "=============================================="
echo "✓ SETUP COMPLETE!"
echo "=============================================="
echo ""
echo "Files ready in output/ directory:"
echo "  - schema-ai.json (Complete schema for AI/ML)"
echo "  - schema-from-server.json (Latest scan data)"
echo "  - er_diagram-from-server.mmd (Mermaid diagram)"
echo "  - er_domains.json (Domain-split diagrams)"
echo ""
echo "Next steps:"
echo "  1. View diagram: open output/er_diagram-from-server.mmd"
echo "  2. Feed to AI: cat output/schema-from-server.json"
echo "  3. Use helper: python3 generate_output_files.py"
echo ""
echo "To stop the application:"
echo "  kill $APP_PID"
echo ""

