package com.yogesh.er_scanner;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class SchemaService {

    private final SchemaScanner scanner;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SchemaService(SchemaScanner scanner) {
        this.scanner = scanner;
    }

    public void regenerateSchema() throws Exception {
        scanner.scan();
    }

    public Object getSchema() throws Exception {
        File file = new File("output/cytoscape_schema.json");

        if (!file.exists()) {
            throw new RuntimeException("Schema file not found. Run /schema/scan first.");
        }

        return objectMapper.readValue(file, Object.class);
    }
}