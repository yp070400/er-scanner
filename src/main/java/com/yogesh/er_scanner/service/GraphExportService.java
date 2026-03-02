package com.yogesh.er_scanner.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yogesh.er_scanner.model.*;

import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GraphExportService {

    private final SchemaService schemaService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GraphExportService(SchemaService schemaService) {
        this.schemaService = schemaService;
    }

    // ============================================================
    // Cytoscape JSON
    // ============================================================

    public Map<String, Object> getCytoscapeGraph() throws Exception {

        Schema schema = schemaService.getSchema();
        Map<String, String> domainMap = loadDomainMap(schema);

        List<Map<String, Object>> nodes = new ArrayList<>();
        for (Table table : schema.getTables()) {
            String name = table.getName();
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", name);
            data.put("label", name.toUpperCase());
            data.put("domain", domainMap.getOrDefault(name.toLowerCase(), "Uncategorized"));
            data.put("columns", table.getColumns().stream()
                    .map(Column::getName)
                    .collect(Collectors.toList()));
            data.put("tableType", "business");

            Map<String, Object> node = new LinkedHashMap<>();
            node.put("data", data);
            nodes.add(node);
        }

        List<Map<String, Object>> edges = new ArrayList<>();
        for (Relationship r : schema.getRelationships()) {
            String edgeId = r.getSourceTable() + "-" + r.getTargetTable()
                    + "-" + r.getSourceColumn();

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", edgeId);
            data.put("source", r.getSourceTable());
            data.put("target", r.getTargetTable());
            data.put("sourceColumn", r.getSourceColumn());
            data.put("targetColumn", r.getTargetColumn());
            data.put("relationshipType",
                    r.getRelationshipType() != null ? r.getRelationshipType().name() : "UNKNOWN");
            data.put("confidence", r.getConfidence());

            Map<String, Object> edge = new LinkedHashMap<>();
            edge.put("data", data);
            edges.add(edge);
        }

        Map<String, Object> graph = new LinkedHashMap<>();
        graph.put("nodes", nodes);
        graph.put("edges", edges);
        return graph;
    }

    // ============================================================
    // GraphML Export
    // ============================================================

    public String getGraphMl() throws Exception {

        Schema schema = schemaService.getSchema();
        Map<String, String> domainMap = loadDomainMap(schema);

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<graphml xmlns=\"http://graphml.graphdrawing.org/graphml\"\n");
        sb.append("         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        sb.append("         xsi:schemaLocation=\"http://graphml.graphdrawing.org/graphml\n");
        sb.append("           http://graphml.graphdrawing.org/graphml/1.0/graphml.xsd\">\n\n");

        // Node attribute keys
        sb.append("  <key id=\"domain\" for=\"node\" attr.name=\"domain\" attr.type=\"string\"/>\n");
        sb.append("  <key id=\"tableType\" for=\"node\" attr.name=\"tableType\" attr.type=\"string\"/>\n");
        sb.append("  <key id=\"columns\" for=\"node\" attr.name=\"columns\" attr.type=\"string\"/>\n");

        // Edge attribute keys
        sb.append("  <key id=\"relationshipType\" for=\"edge\" attr.name=\"relationshipType\" attr.type=\"string\"/>\n");
        sb.append("  <key id=\"confidence\" for=\"edge\" attr.name=\"confidence\" attr.type=\"double\"/>\n");
        sb.append("  <key id=\"sourceColumn\" for=\"edge\" attr.name=\"sourceColumn\" attr.type=\"string\"/>\n");
        sb.append("  <key id=\"targetColumn\" for=\"edge\" attr.name=\"targetColumn\" attr.type=\"string\"/>\n\n");

        sb.append("  <graph id=\"G\" edgedefault=\"directed\">\n\n");

        for (Table table : schema.getTables()) {
            String name = table.getName();
            String domain = domainMap.getOrDefault(name.toLowerCase(), "Uncategorized");
            String cols = table.getColumns().stream()
                    .map(Column::getName)
                    .collect(Collectors.joining(","));

            sb.append("    <node id=\"").append(escapeXml(name)).append("\">\n");
            sb.append("      <data key=\"domain\">").append(escapeXml(domain)).append("</data>\n");
            sb.append("      <data key=\"tableType\">business</data>\n");
            sb.append("      <data key=\"columns\">").append(escapeXml(cols)).append("</data>\n");
            sb.append("    </node>\n");
        }

        sb.append("\n");

        int edgeIdx = 0;
        for (Relationship r : schema.getRelationships()) {
            String type = r.getRelationshipType() != null
                    ? r.getRelationshipType().name() : "UNKNOWN";

            sb.append("    <edge id=\"e").append(edgeIdx++).append("\"");
            sb.append(" source=\"").append(escapeXml(r.getSourceTable())).append("\"");
            sb.append(" target=\"").append(escapeXml(r.getTargetTable())).append("\">\n");
            sb.append("      <data key=\"relationshipType\">").append(type).append("</data>\n");
            sb.append("      <data key=\"confidence\">").append(r.getConfidence()).append("</data>\n");
            sb.append("      <data key=\"sourceColumn\">").append(escapeXml(r.getSourceColumn())).append("</data>\n");
            sb.append("      <data key=\"targetColumn\">").append(escapeXml(r.getTargetColumn())).append("</data>\n");
            sb.append("    </edge>\n");
        }

        sb.append("\n  </graph>\n</graphml>\n");
        return sb.toString();
    }

    // ============================================================
    // Helpers
    // ============================================================

    private Map<String, String> loadDomainMap(Schema schema) {
        Map<String, String> tableToDomainsMap = new LinkedHashMap<>();

        File domainsFile = new File("output/domains.json");
        if (domainsFile.exists()) {
            try {
                Map<String, List<String>> domainGroups = objectMapper.readValue(
                        domainsFile,
                        new TypeReference<>() {});

                for (Map.Entry<String, List<String>> entry : domainGroups.entrySet()) {
                    for (String table : entry.getValue()) {
                        tableToDomainsMap.put(table.toLowerCase(), entry.getKey());
                    }
                }
            } catch (Exception e) {
                System.err.println("[GraphExportService] Could not read domains.json: " + e.getMessage());
            }
        }

        return tableToDomainsMap;
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
