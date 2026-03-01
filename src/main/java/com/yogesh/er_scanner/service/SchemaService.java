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

        writeAiJson();
    }

    // ============================================================
    // 3️⃣ WRITE schema-ai.json
    // ============================================================

    public void writeAiJson() throws Exception {

        ObjectMapper mapper = new ObjectMapper();

        File dir = new File("output");
        if (!dir.exists()) dir.mkdirs();

        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(
                        new File("output/schema-ai.json"),
                        this.schema
                );
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

    public Map<String, String> generateMermaidChunks(
            int maxTablesPerChunk) {

        try {

            ObjectMapper mapper = new ObjectMapper();

            Map<String, List<String>> domainMap =
                    mapper.readValue(
                            new File("output/domains.json"),
                            new TypeReference<>() {}
                    );

            Map<String, String> result =
                    new LinkedHashMap<>();

            for (Map.Entry<String, List<String>> entry :
                    domainMap.entrySet()) {

                String domainName = entry.getKey();
                List<String> domainTables = entry.getValue();

                List<Table> tablesInDomain =
                        schema.getTables().stream()
                                .filter(t ->
                                        domainTables.contains(
                                                t.getName()))
                                .collect(Collectors.toList());

                if (tablesInDomain.isEmpty()) continue;

                for (int i = 0;
                     i < tablesInDomain.size();
                     i += maxTablesPerChunk) {

                    List<Table> chunk =
                            tablesInDomain.subList(
                                    i,
                                    Math.min(
                                            i + maxTablesPerChunk,
                                            tablesInDomain.size()
                                    )
                            );

                    Set<String> chunkNames =
                            chunk.stream()
                                    .map(Table::getName)
                                    .collect(Collectors.toSet());

                    List<Relationship> rels =
                            schema.getRelationships().stream()
                                    .filter(r ->
                                            chunkNames.contains(
                                                    r.getSourceTable())
                                                    &&
                                                    chunkNames.contains(
                                                            r.getTargetTable()))
                                    .collect(Collectors.toList());

                    String mermaid =
                            buildMermaid(chunk, rels);

                    String key = domainName;

                    if (tablesInDomain.size() >
                            maxTablesPerChunk) {

                        key = domainName + " - Part "
                                + (i / maxTablesPerChunk + 1);
                    }

                    result.put(key, mermaid);
                }
            }

            return result;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to generate Mermaid domain chunks",
                    e
            );
        }
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

            sb.append("    ")
                    .append(r.getSourceTable().toUpperCase())
                    .append(" ||--o{ ")
                    .append(r.getTargetTable().toUpperCase())
                    .append(" : ")
                    .append(r.getSourceColumn())
                    .append("\n");
        }

        return sb.toString();
    }
}