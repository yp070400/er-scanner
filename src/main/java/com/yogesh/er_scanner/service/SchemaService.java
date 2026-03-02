package com.yogesh.er_scanner.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yogesh.er_scanner.model.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SchemaService {

    private Schema schema;

    @Autowired
    private DataSampleService dataSampleService;

    // ============================================================
    // 1️⃣ SET / GET SCHEMA
    // ============================================================

    public void setSchema(Schema schema) {
        this.schema = schema;
    }

    public Schema getSchema() {
        if (schema == null) {
            throw new RuntimeException(
                    "Schema not found. Run /schema/scan first.");
        }
        return schema;
    }

    // ============================================================
    // 2️⃣ BUILD FULL SCHEMA (Metadata + Sample Relationships)
    // ============================================================

    public void buildSchema(Schema metadataSchema) throws Exception {

        List<Relationship> mergedRelationships =
                new ArrayList<>(metadataSchema.getRelationships());

        // Extract table names
        List<String> tableNames = metadataSchema.getTables()
                .stream()
                .map(Table::getName)
                .collect(Collectors.toList());

        // Detect sample relationships
        List<Relationship> sampleRelationships =
                dataSampleService.detectSampleRelationships(
                        tableNames,
                        10 // configurable if needed
                );

        mergedRelationships.addAll(sampleRelationships);

        this.schema = new Schema(
                metadataSchema.getTables(),
                mergedRelationships
        );

        System.out.println("\n[SchemaService] Writing output files...");
        try {
            writeAiJson();
        } catch (Exception e) {
            System.err.println("ERROR writing schema-ai.json: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            writeMermaidDiagram();
        } catch (Exception e) {
            System.err.println("ERROR writing mermaid diagram: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            writeMermaidDomains();
        } catch (Exception e) {
            System.err.println("ERROR writing mermaid domains: " + e.getMessage());
        }
        System.out.println("[SchemaService] File writing complete.\n");
    }

    // ============================================================
    // 3️⃣ WRITE schema-ai.json
    // ============================================================

    public void writeAiJson() throws Exception {

        ObjectMapper mapper = new ObjectMapper();

        File dir = new File("output");
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created && !dir.exists()) {
                throw new RuntimeException("Failed to create output directory: " + dir.getAbsolutePath());
            }
        }

        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(
                        new File("output/schema-ai.json"),
                        this.schema
                );
        System.out.println("✓ Written: output/schema-ai.json");
    }

    // ============================================================
    // 3️⃣ WRITE MERMAID DIAGRAM
    // ============================================================

    public void writeMermaidDiagram() throws Exception {
        String mermaidContent = generateMermaid();
        File dir = new File("output");
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new RuntimeException("Failed to create output directory");
            }
        }
        java.nio.file.Files.write(
                java.nio.file.Paths.get("output/er_diagram-from-server.mmd"),
                mermaidContent.getBytes()
        );
        System.out.println("✓ Written: output/er_diagram-from-server.mmd");
    }

    // ============================================================
    // 3b️⃣ WRITE MERMAID DOMAINS
    // ============================================================

    public void writeMermaidDomains() {
        ObjectMapper mapper = new ObjectMapper();

        File dir = new File("output");
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new RuntimeException("Failed to create output directory");
            }
        }

        try {
            Map<String, String> domains = generateMermaidChunks(20);
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(
                            new File("output/er_domains.json"),
                            domains
                    );
            System.out.println("✓ Written: output/er_domains.json");
        } catch (Exception e) {
            System.out.println("⚠ Warning: Could not generate domain diagrams: " + e.getMessage());
            System.out.println("  This is okay - full mermaid diagram is still generated.");
        }
    }

    // ============================================================
    // 4️⃣ FULL MERMAID (ALL TABLES)
    // ============================================================

    public String generateMermaid() {

        Schema s = getSchema();

        return buildMermaid(
                s.getTables(),
                s.getRelationships()
        );
    }

    // ============================================================
    // 5️⃣ DOMAIN-BASED MERMAID CHUNKS
    // ============================================================

    // ============================================================
// DOMAIN-BASED CHUNKED MERMAID
// ============================================================

    public Map<String, String> generateMermaidChunks(int maxTablesPerChunk) {

        Schema schema = getSchema();

        Map<String, String> result = new LinkedHashMap<>();

        try {

            ObjectMapper mapper = new ObjectMapper();

            Map<String, List<String>> domainMap =
                    mapper.readValue(
                            new File("output/domains.json"),
                            new TypeReference<>() {}
                    );

            for (Map.Entry<String, List<String>> entry : domainMap.entrySet()) {

                String domainName = entry.getKey();

                List<Table> domainTables =
                        schema.getTables()
                                .stream()
                                .filter(t ->
                                        entry.getValue()
                                                .contains(t.getName()))
                                .toList();

                if (domainTables.isEmpty())
                    continue;

                // ---- SUB-CHUNKING ----
                for (int i = 0; i < domainTables.size();
                     i += maxTablesPerChunk) {

                    List<Table> chunk =
                            domainTables.subList(
                                    i,
                                    Math.min(
                                            i + maxTablesPerChunk,
                                            domainTables.size()
                                    )
                            );

                    // Filter relationships inside this chunk
                    Set<String> chunkNames =
                            chunk.stream()
                                    .map(Table::getName)
                                    .collect(Collectors.toSet());

                    List<Relationship> chunkRelationships =
                            schema.getRelationships()
                                    .stream()
                                    .filter(r ->
                                            chunkNames.contains(
                                                    r.getSourceTable())
                                                    &&
                                                    chunkNames.contains(
                                                            r.getTargetTable()))
                                    .toList();

                    String mermaid =
                            buildMermaidFiltered(
                                    chunk,
                                    chunkRelationships
                            );

                    if (mermaid.length() < 30)
                        continue;

                    String key =
                            domainName
                                    + (domainTables.size()
                                    > maxTablesPerChunk
                                    ? " - Part "
                                    + (i / maxTablesPerChunk + 1)
                                    : "");

                    result.put(key, mermaid);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(
                    "Mermaid chunk generation failed", e);
        }

        return result;
    }

    private String buildMermaidFiltered(
            List<Table> tables,
            List<Relationship> relationships) {

        StringBuilder sb = new StringBuilder();

        sb.append("erDiagram\n");
        sb.append("    direction TB\n\n");

        // Collect important columns per table
        Map<String, Set<String>> importantColumns =
                new HashMap<>();

        for (Relationship r : relationships) {

            importantColumns
                    .computeIfAbsent(
                            r.getSourceTable(),
                            k -> new HashSet<>())
                    .add(r.getSourceColumn());

            importantColumns
                    .computeIfAbsent(
                            r.getTargetTable(),
                            k -> new HashSet<>())
                    .add(r.getTargetColumn());
        }

        // ---- TABLE DEFINITIONS ----
        for (Table table : tables) {

            sb.append("    ")
                    .append(table.getName().toUpperCase())
                    .append(" {\n");

            for (Column column : table.getColumns()) {

                boolean isImportant =
                        column.isPrimaryKey()
                                || column.isForeignKey()
                                || importantColumns
                                .getOrDefault(
                                        table.getName(),
                                        Collections.emptySet())
                                .contains(column.getName());

                if (!isImportant)
                    continue;

                sb.append("        string ")
                        .append(column.getName());

                if (column.isPrimaryKey()) {
                    sb.append(" PK");
                } else if (column.isForeignKey()) {
                    sb.append(" FK");
                }

                sb.append("\n");
            }

            sb.append("    }\n\n");
        }

        // ---- RELATIONSHIPS ----
        for (Relationship r : relationships) {

            // Prepare a label that includes relationship type if not STRICT
            String label = r.getSourceColumn();
            if (r.getRelationshipType() != null) {
                switch (r.getRelationshipType()) {
                    case DATA_INFERRED:
                        label = label + " (data-inferred)";
                        break;
                    case DATA_SAMPLE:
                        label = label + " (data-sample)";
                        break;
                    case INFERRED:
                        label = label + " (inferred)";
                        break;
                    default:
                        // STRICT or unknown -> keep label as-is
                        break;
                }
            }

            sb.append("    ")
                    .append(r.getSourceTable().toUpperCase())
                    .append(" ||--o{ ")
                    .append(r.getTargetTable().toUpperCase())
                    .append(" : ")
                    .append(label)
                    .append("\n");
        }

        return sb.toString();
    }

    // ============================================================
    // 6️⃣ INTERNAL MERMAID BUILDER
    // ============================================================

    private String buildMermaid(List<Table> tables,
                                List<Relationship> relationships) {

        StringBuilder sb = new StringBuilder();

        sb.append("erDiagram\n");
        sb.append("    direction TB\n\n");

        // TABLES
        for (Table table : tables) {

            sb.append("    ")
                    .append(table.getName().toUpperCase())
                    .append(" {\n");

            for (Column column : table.getColumns()) {

                sb.append("        string ")
                        .append(column.getName());

                if (column.isPrimaryKey()) {
                    sb.append(" PK");
                } else if (column.isForeignKey()) {
                    sb.append(" FK");
                }

                sb.append("\n");
            }

            sb.append("    }\n\n");
        }

        // RELATIONSHIPS
        for (Relationship r : relationships) {

            String label = r.getSourceColumn();
            if (r.getRelationshipType() != null) {
                switch (r.getRelationshipType()) {
                    case DATA_INFERRED:
                        label = label + " (data-inferred)";
                        break;
                    case DATA_SAMPLE:
                        label = label + " (data-sample)";
                        break;
                    case INFERRED:
                        label = label + " (inferred)";
                        break;
                    default:
                        break;
                }
            }

            sb.append("    ")
                    .append(r.getSourceTable().toUpperCase())
                    .append(" ||--o{ ")
                    .append(r.getTargetTable().toUpperCase())
                    .append(" : ")
                    .append(label)
                    .append("\n");
        }

        return sb.toString();
    }
}
