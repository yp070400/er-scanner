package com.yogesh.er_scanner.controller;

import com.yogesh.er_scanner.model.Schema;
import com.yogesh.er_scanner.service.SchemaScanner;
import com.yogesh.er_scanner.service.SchemaService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.*;

@RestController
@RequestMapping("/schema")
public class SchemaController {

    private final SchemaScanner schemaScanner;
    private final SchemaService schemaService;

    public SchemaController(SchemaScanner schemaScanner,
                            SchemaService schemaService) {
        this.schemaScanner = schemaScanner;
        this.schemaService = schemaService;
    }

    // =====================================================
    // 1️⃣ Scan Configured Tables Only
    // =====================================================

    @PostMapping("/scan")
    public ResponseEntity<String> scanSchema() {

        try {

            Schema scannedSchema = schemaScanner.scan();

            // This merges DATA_SAMPLE relationships
            schemaService.buildSchema(scannedSchema);

            return ResponseEntity.ok(
                    "Schema scanned successfully for configured tables.");

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.internalServerError()
                    .body("Scan failed: " + e.getMessage());
        }
    }

    // =====================================================
    // 2️⃣ Get Full Schema JSON (AI Compatible)
    // =====================================================

    @GetMapping("/json")
    public ResponseEntity<Schema> getSchemaJson() {

        return ResponseEntity.ok(
                schemaService.getSchema());
    }

    // =====================================================
    // 3️⃣ Get Mermaid ER Diagram
    // =====================================================

    @GetMapping("/er-mermaid")
    public ResponseEntity<String> getMermaid() {
        System.out.println("Generating Mermaid ER Diagram..."+schemaService.generateMermaid());
        return ResponseEntity.ok(
                schemaService.generateMermaid());
    }

    // =====================================================
    // 4️⃣ Get Domain-Based Mermaid Chunks (Optional)
    // =====================================================

    @GetMapping("/er-mermaid-domains")
    public ResponseEntity<List<String>> listDomainFiles() {

        File dir = new File("output/mermaid-domains");

        if (!dir.exists()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<String> files = Arrays.stream(Objects.requireNonNull(dir.listFiles()))
                .filter(f -> f.getName().endsWith(".mmd"))
                .map(File::getName)
                .sorted()
                .toList();

        return ResponseEntity.ok(files);
    }
}