package com.yogesh.er_scanner.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yogesh.er_scanner.model.*;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;

@Service
public class SchemaService {

    private Schema schema;

    public void setSchema(Schema schema) {
        this.schema = schema;
    }

    public Schema getSchema() {
        if (schema == null) {
            throw new RuntimeException("Schema not found. Run /schema/scan first.");
        }
        return schema;
    }

    // ================= VISUAL JSON =================
    public Map<String, Object> generateVisualJson() {

        Schema s = getSchema();

        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        for (Table t : s.getTables()) {

            Map<String, Object> data = new HashMap<>();
            data.put("id", t.getName());
            data.put("label", t.getName());
            data.put("columns", t.getColumns());
            data.put("primaryKeys", t.getPrimaryKeys());

            nodes.add(Map.of("data", data));
        }

        for (Relationship r : s.getRelationships()) {

            Map<String, Object> data = new HashMap<>();
            data.put("id", r.getSource() + "_" + r.getTarget() + "_" + r.getColumn());
            data.put("source", r.getSource());
            data.put("target", r.getTarget());
            data.put("column", r.getColumn());
            data.put("type", r.getType());
            data.put("confidence", r.getConfidence());

            edges.add(Map.of("data", data));
        }

        return Map.of("nodes", nodes, "edges", edges);
    }

    // ================= AI JSON FILE =================
    public void writeAiJson() throws Exception {

        ObjectMapper mapper = new ObjectMapper();

        File dir = new File("output");
        if (!dir.exists()) dir.mkdirs();

        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(new File("output/schema-ai.json"), getSchema());
    }

    // ================= MERMAID ER =================
    public String generateMermaid() {

        StringBuilder sb = new StringBuilder();
        sb.append("erDiagram\n");

        Schema s = getSchema();

        for (Table t : s.getTables()) {

            sb.append("    ").append(t.getName().toUpperCase()).append(" {\n");

            for (String col : t.getColumns()) {

                if (t.getPrimaryKeys().contains(col)) {
                    sb.append("        string ").append(col).append(" PK\n");
                } else {
                    sb.append("        string ").append(col).append("\n");
                }
            }

            sb.append("    }\n\n");
        }

        for (Relationship r : s.getRelationships()) {

            sb.append("    ")
                    .append(r.getSource().toUpperCase())
                    .append(" ||--o{ ")
                    .append(r.getTarget().toUpperCase())
                    .append(" : ")
                    .append(r.getColumn())
                    .append("\n");
        }

        return sb.toString();
    }
}