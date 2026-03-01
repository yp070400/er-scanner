package com.yogesh.er_scanner.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yogesh.er_scanner.model.*;

import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SchemaService {

    private Schema schema;

    // ================== SCHEMA STATE ==================

    public void setSchema(Schema schema) {
        this.schema = schema;
    }

    public Schema getSchema() {
        if (schema == null) {
            throw new RuntimeException("Schema not found. Run /schema/scan first.");
        }
        return schema;
    }

    // ================== VISUAL JSON (Cytoscape) ==================

    public Map<String, Object> generateVisualJson() {

        Schema s = getSchema();

        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        for (Table table : s.getTables()) {

            Map<String, Object> data = new HashMap<>();
            data.put("id", table.getName());
            data.put("label", table.getName());
            data.put("columns", table.getColumns());
            data.put("primaryKeys",
                    table.getColumns().stream()
                            .filter(Column::isPrimaryKey)
                            .map(Column::getName)
                            .collect(Collectors.toList())
            );

            nodes.add(Map.of("data", data));
        }

        for (Relationship r : s.getRelationships()) {

            Map<String, Object> data = new HashMap<>();
            data.put("id",
                    r.getSourceTable() + "_"
                            + r.getTargetTable() + "_"
                            + r.getSourceColumn());

            data.put("source", r.getSourceTable());
            data.put("target", r.getTargetTable());
            data.put("column", r.getSourceColumn());
            data.put("type", r.getType());
            data.put("confidence", r.getConfidence());

            edges.add(Map.of("data", data));
        }

        return Map.of("nodes", nodes, "edges", edges);
    }

    // ================== WRITE AI JSON ==================

    public void writeAiJson() throws Exception {

        ObjectMapper mapper = new ObjectMapper();

        File dir = new File("output");
        if (!dir.exists()) dir.mkdirs();

        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(new File("output/schema-ai.json"), getSchema());
    }

    // ================== FULL MERMAID ==================

    public String generateMermaid() {

        Schema s = getSchema();

        return buildMermaidFromTables(s.getTables(), s.getRelationships());
    }

    // ================== DOMAIN-BASED MERMAID CHUNKS ==================

    public Map<String, String> generateMermaidChunks(int maxTablesPerChunk) {

        try {

            ObjectMapper mapper = new ObjectMapper();

            Schema schema = mapper.readValue(
                    new File("output/schema-ai.json"),
                    Schema.class
            );

            Map<String, List<String>> domainMap =
                    mapper.readValue(
                            new File("output/domains.json"),
                            new TypeReference<Map<String, List<String>>>() {}
                    );

            Map<String, String> result = new LinkedHashMap<>();

            for (Map.Entry<String, List<String>> entry : domainMap.entrySet()) {

                String domainName = entry.getKey();
                List<String> domainTables = entry.getValue();

                // Filter real table objects
                List<Table> tablesInDomain = schema.getTables().stream()
                        .filter(t -> domainTables.contains(t.getName()))
                        .collect(Collectors.toList());

                if (tablesInDomain.isEmpty()) continue;

                // 🔥 CASE 1: SMALL DOMAIN (No Chunking Needed)
                if (tablesInDomain.size() <= maxTablesPerChunk) {

                    Set<String> tableNames = tablesInDomain.stream()
                            .map(Table::getName)
                            .collect(Collectors.toSet());

                    List<Relationship> relationships = schema.getRelationships().stream()
                            .filter(r ->
                                    tableNames.contains(r.getSourceTable())
                                            && tableNames.contains(r.getTargetTable()))
                            .collect(Collectors.toList());

                    String mermaid = buildMermaidFromTables(tablesInDomain, relationships);

                    result.put(domainName, mermaid);
                }

                // 🔥 CASE 2: LARGE DOMAIN (Chunk It)
                else {

                    int part = 1;

                    for (int i = 0; i < tablesInDomain.size(); i += maxTablesPerChunk) {

                        List<Table> chunk =
                                tablesInDomain.subList(
                                        i,
                                        Math.min(i + maxTablesPerChunk, tablesInDomain.size())
                                );

                        Set<String> chunkTableNames = chunk.stream()
                                .map(Table::getName)
                                .collect(Collectors.toSet());

                        List<Relationship> relationships = schema.getRelationships().stream()
                                .filter(r ->
                                        chunkTableNames.contains(r.getSourceTable())
                                                && chunkTableNames.contains(r.getTargetTable()))
                                .collect(Collectors.toList());

                        String mermaid = buildMermaidFromTables(chunk, relationships);

                        result.put(domainName + " - Part " + part, mermaid);

                        part++;
                    }
                }
            }

            return result;

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Mermaid domain diagrams", e);
        }
    }

    // ================== INTERNAL MERMAID BUILDER ==================

    private String buildMermaidFromTables(List<Table> tables,
                                          List<Relationship> relationships) {

        StringBuilder sb = new StringBuilder();
        sb.append("erDiagram\n");

        // ----- TABLES -----
        for (Table table : tables) {

            sb.append("    ")
                    .append(table.getName().toUpperCase())
                    .append(" {\n");

            for (Column column : table.getColumns()) {

                // 🔥 Always use safe type to prevent Mermaid syntax errors
                sb.append("        string ")
                        .append(column.getName());

                if (column.isPrimaryKey()) {
                    sb.append(" PK");
                }

                sb.append("\n");
            }

            sb.append("    }\n\n");
        }

        // ----- RELATIONSHIPS -----
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