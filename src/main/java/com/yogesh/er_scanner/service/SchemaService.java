package com.yogesh.er_scanner.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yogesh.er_scanner.config.RelationshipConfig;
import com.yogesh.er_scanner.model.*;

import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SchemaService {

    private Schema schema;

    private final DataSampleService dataSampleService;
    private final SemanticRelationshipDetector semanticDetector;
    private final RelationshipConfig config;

    public SchemaService(DataSampleService dataSampleService,
                         SemanticRelationshipDetector semanticDetector,
                         RelationshipConfig config) {
        this.dataSampleService = dataSampleService;
        this.semanticDetector = semanticDetector;
        this.config = config;
    }

    // ============================================================
    // 1️⃣ SET / GET SCHEMA
    // ============================================================

    public void setSchema(Schema schema) {
        this.schema = schema;
    }

    public Schema getSchema() {
        if (schema == null) {
            throw new RuntimeException("Schema not found. Run /schema/scan first.");
        }
        return schema;
    }

    // ============================================================
    // 2️⃣ BUILD FULL SCHEMA
    // ============================================================

    public void buildSchema(Schema metadataSchema) throws Exception {

        List<Relationship> mergedRelationships =
                new ArrayList<>(metadataSchema.getRelationships());

        List<String> tableNames = metadataSchema.getTables()
                .stream()
                .map(Table::getName)
                .collect(Collectors.toList());

        List<Relationship> sampleRelationships =
                dataSampleService.detectSampleRelationships(tableNames, config.getSampleSize());

        List<Relationship> semanticRelationships =
                semanticDetector.detect(metadataSchema.getTables());

        mergedRelationships.addAll(sampleRelationships);
        mergedRelationships.addAll(semanticRelationships);

        mergedRelationships = deduplicate(mergedRelationships);

        this.schema = new Schema(
                metadataSchema.getTables(),
                mergedRelationships
        );

        System.out.println("\n[SchemaService] Writing output files...");

        writeAiJson();
        writeMermaidDiagram();
        splitAndWriteDomainChunks(20); // 🔥 uses domains.json

        System.out.println("[SchemaService] File writing complete.\n");
    }

    // ============================================================
    // 3️⃣ WRITE schema-ai.json
    // ============================================================

    public void writeAiJson() throws Exception {

        ObjectMapper mapper = new ObjectMapper();

        File dir = new File("output");
        if (!dir.exists()) dir.mkdirs();

        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(new File("output/schema-ai.json"), this.schema);

        System.out.println("✓ Written: output/schema-ai.json");
    }

    // ============================================================
    // 4️⃣ WRITE FULL MERMAID
    // ============================================================

    public void writeMermaidDiagram() throws Exception {

        String mermaidContent = generateMermaid();

        File dir = new File("output");
        if (!dir.exists()) dir.mkdirs();

        Files.write(
                Paths.get("output/er_diagram-from-server.mmd"),
                mermaidContent.getBytes()
        );

        System.out.println("✓ Written: output/er_diagram-from-server.mmd");
    }

    // ============================================================
    // 5️⃣ SPLIT USING AI domains.json
    // ============================================================

    public void splitAndWriteDomainChunks(int maxTablesPerChunk) throws Exception {

        String fullMermaid = Files.readString(
                Paths.get("output/er_diagram-from-server.mmd")
        );

        Map<String, String> tableBlocks = extractTableBlocks(fullMermaid);
        List<String> relationships = extractRelationshipLines(fullMermaid);

        File domainsFile = new File("output/domains.json");
        if (!domainsFile.exists()) {
            System.out.println("[SchemaService] domains.json not found — skipping domain chunk split.");
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        Map<String, List<String>> domainMap =
                mapper.readValue(domainsFile, new TypeReference<>() {});

        File domainDir = new File("output/mermaid-domains");
        if (!domainDir.exists()) domainDir.mkdirs();

        for (Map.Entry<String, List<String>> entry : domainMap.entrySet()) {

            String domainName = sanitizeFileName(entry.getKey());

            List<String> domainTablesUpper =
                    entry.getValue().stream()
                            .map(String::toUpperCase)
                            .toList();

            List<String> existingTables =
                    domainTablesUpper.stream()
                            .filter(tableBlocks::containsKey)
                            .toList();

            if (existingTables.isEmpty()) continue;

            for (int i = 0; i < existingTables.size(); i += maxTablesPerChunk) {

                List<String> chunkTables =
                        existingTables.subList(
                                i,
                                Math.min(i + maxTablesPerChunk,
                                        existingTables.size())
                        );

                Set<String> chunkSet = new HashSet<>(chunkTables);

                List<String> chunkRelationships =
                        relationships.stream()
                                .filter(r ->
                                        chunkSet.stream()
                                                .anyMatch(r::contains))
                                .toList();

                String chunkMermaid =
                        buildChunkMermaid(
                                chunkTables,
                                tableBlocks,
                                chunkRelationships
                        );

                String fileName =
                        domainName
                                + (existingTables.size() > maxTablesPerChunk
                                ? "_Part_" + (i / maxTablesPerChunk + 1)
                                : "")
                                + ".mmd";

                Files.write(
                        Paths.get("output/mermaid-domains/" + fileName),
                        chunkMermaid.getBytes()
                );

                System.out.println("✓ Written: " + fileName);
            }
        }
    }

    // ============================================================
    // 6️⃣ DEDUPLICATION
    // ============================================================

    private List<Relationship> deduplicate(List<Relationship> relationships) {

        // Priority: STRICT > DATA_INFERRED > INFERRED > DATA_SAMPLE
        Map<RelationshipType, Integer> priority = new EnumMap<>(RelationshipType.class);
        priority.put(RelationshipType.STRICT, 4);
        priority.put(RelationshipType.DATA_INFERRED, 3);
        priority.put(RelationshipType.INFERRED, 2);
        priority.put(RelationshipType.DATA_SAMPLE, 1);

        // Canonical key: sorted pair so (A→B) and (B→A) are same
        Map<String, Relationship> best = new LinkedHashMap<>();

        for (Relationship r : relationships) {
            String key = canonicalKey(r);
            Relationship existing = best.get(key);

            if (existing == null) {
                best.put(key, r);
            } else {
                int newPri = priority.getOrDefault(r.getRelationshipType(), 0);
                int exPri = priority.getOrDefault(existing.getRelationshipType(), 0);

                if (newPri > exPri
                        || (newPri == exPri && r.getConfidence() > existing.getConfidence())) {
                    best.put(key, r);
                }
            }
        }

        return new ArrayList<>(best.values());
    }

    private String canonicalKey(Relationship r) {
        String a = r.getSourceTable().toLowerCase() + "." + r.getSourceColumn().toLowerCase();
        String b = r.getTargetTable().toLowerCase() + "." + r.getTargetColumn().toLowerCase();
        return a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;
    }

    // ============================================================
    // 7️⃣ FULL MERMAID BUILDER
    // ============================================================

    public String generateMermaid() {
        Schema s = getSchema();
        return buildMermaid(s.getTables(), s.getRelationships());
    }

    private String buildMermaid(List<Table> tables,
                                List<Relationship> relationships) {

        StringBuilder sb = new StringBuilder();

        sb.append("erDiagram\n");
        sb.append("    direction TB\n\n");

        for (Table table : tables) {

            sb.append("    ").append(table.getName().toUpperCase()).append(" {\n");

            for (Column column : table.getColumns()) {

                sb.append("        string ").append(column.getName());

                if (column.isPrimaryKey()) sb.append(" PK");
                else if (column.isForeignKey()) sb.append(" FK");

                sb.append("\n");
            }

            sb.append("    }\n\n");
        }

        for (Relationship r : relationships) {

            String label = r.getSourceColumn();

//            if (r.getRelationshipType() != null) {
//                label += " (" + r.getRelationshipType().name().toLowerCase() + ")";
//            }

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
    // 8️⃣ HELPERS
    // ============================================================

    private Map<String, String> extractTableBlocks(String content) {

        Map<String, String> tableBlocks = new LinkedHashMap<>();

        String[] lines = content.split("\n");
        String currentTable = null;
        StringBuilder buffer = new StringBuilder();

        for (String line : lines) {

            String trimmed = line.trim();

            if (trimmed.endsWith("{")) {
                currentTable = trimmed.replace("{", "").trim();
                buffer = new StringBuilder();
                buffer.append("    ").append(trimmed).append("\n");
            }
            else if (currentTable != null) {
                buffer.append("    ").append(trimmed).append("\n");

                if (trimmed.equals("}")) {
                    tableBlocks.put(currentTable, buffer.toString());
                    currentTable = null;
                }
            }
        }

        return tableBlocks;
    }

    private List<String> extractRelationshipLines(String content) {

        return Arrays.stream(content.split("\n"))
                .map(String::trim)
                .filter(line -> line.contains("||--"))
                .toList();
    }

    private String buildChunkMermaid(
            List<String> tables,
            Map<String, String> tableBlocks,
            List<String> relationships) {

        StringBuilder sb = new StringBuilder();

        sb.append("erDiagram\n");
        sb.append("    direction TB\n\n");

        for (String table : tables) {
            sb.append(tableBlocks.get(table)).append("\n");
        }

        for (String rel : relationships) {
            sb.append("    ").append(rel).append("\n");
        }

        return sb.toString();
    }

    private String sanitizeFileName(String input) {
        return input.replaceAll("[^a-zA-Z0-9_]", "_");
    }
}