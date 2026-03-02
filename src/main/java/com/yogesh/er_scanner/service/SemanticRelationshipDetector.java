package com.yogesh.er_scanner.service;

import com.yogesh.er_scanner.model.*;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Detects FK relationships based on column naming conventions,
 * without touching the database at all.
 */
@Service
public class SemanticRelationshipDetector {

    /**
     * Main entry point — returns INFERRED relationships detected via naming patterns.
     */
    public List<Relationship> detect(List<Table> tables) {

        // Step 1: build PK index  { tableName → pkColumnName }
        Map<String, String> pkIndex = buildPkIndex(tables);

        // Step 2: collect existing STRICT relationships to avoid duplicates
        Set<String> strictKeys = new HashSet<>(); // filled by SchemaService before dedup

        List<Relationship> results = new ArrayList<>();

        for (Table table : tables) {
            String tableName = table.getName().toLowerCase();

            for (Column column : table.getColumns()) {
                String colName = column.getName().toLowerCase();

                // Skip the column if it IS a PK of its own table
                if (column.isPrimaryKey()) continue;

                // --- Pattern A: exact PK name match ---
                for (Map.Entry<String, String> entry : pkIndex.entrySet()) {
                    String targetTable = entry.getKey();
                    String targetPk = entry.getValue();

                    if (targetTable.equals(tableName)) continue; // same table

                    if (colName.equals(targetPk.toLowerCase())) {
                        results.add(new Relationship(
                                table.getName(), column.getName(),
                                targetTable, targetPk,
                                RelationshipType.INFERRED, 0.95
                        ));
                    }
                }

                // --- Pattern B: {singular(tableName)}_id ---
                for (Map.Entry<String, String> entry : pkIndex.entrySet()) {
                    String targetTable = entry.getKey();
                    String targetPk = entry.getValue();

                    if (targetTable.equals(tableName)) continue;

                    String singular = singularize(targetTable);
                    String expected = singular + "_id";

                    if (colName.equals(expected) && !colName.equals(targetPk.toLowerCase())) {
                        // Only add if Pattern A didn't already match this combo
                        results.add(new Relationship(
                                table.getName(), column.getName(),
                                targetTable, targetPk,
                                RelationshipType.INFERRED, 0.90
                        ));
                    }
                }

                // --- Pattern C: *_code / *_ref patterns ---
                if (colName.endsWith("_code") || colName.endsWith("_ref")) {
                    String prefix = colName.endsWith("_code")
                            ? colName.substring(0, colName.length() - 5)
                            : colName.substring(0, colName.length() - 4);

                    for (Map.Entry<String, String> entry : pkIndex.entrySet()) {
                        String targetTable = entry.getKey();
                        String targetPk = entry.getValue();

                        if (targetTable.equals(tableName)) continue;

                        // target PK must end with _code or _ref to match
                        if (targetPk.toLowerCase().equals(colName)
                                || targetTable.toLowerCase().startsWith(prefix)) {
                            results.add(new Relationship(
                                    table.getName(), column.getName(),
                                    targetTable, targetPk,
                                    RelationshipType.INFERRED, 0.75
                            ));
                        }
                    }
                }
            }
        }

        return results;
    }

    private Map<String, String> buildPkIndex(List<Table> tables) {
        Map<String, String> index = new LinkedHashMap<>();
        for (Table table : tables) {
            for (Column col : table.getColumns()) {
                if (col.isPrimaryKey()) {
                    index.put(table.getName().toLowerCase(), col.getName());
                    break; // take first PK
                }
            }
        }
        return index;
    }

    /**
     * Very simple English singularizer covering common DB naming conventions.
     * Handles: -ies → -y, -ses/-shes/-ches → drop -es, -s → drop -s.
     */
    private String singularize(String word) {
        if (word.endsWith("ies")) {
            return word.substring(0, word.length() - 3) + "y";
        }
        if (word.endsWith("ses") || word.endsWith("shes") || word.endsWith("ches")) {
            return word.substring(0, word.length() - 2); // drop 'es'
        }
        if (word.endsWith("s") && !word.endsWith("ss")) {
            return word.substring(0, word.length() - 1);
        }
        return word;
    }
}
