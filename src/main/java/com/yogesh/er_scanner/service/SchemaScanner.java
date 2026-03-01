package com.yogesh.er_scanner.service;

import com.yogesh.er_scanner.model.*;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@Service
public class SchemaScanner {

    private final DataSource dataSource;

    public SchemaScanner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Schema scan() throws Exception {

        List<Table> tables = new ArrayList<>();
        List<Relationship> relationships = new ArrayList<>();

        try (Connection conn = dataSource.getConnection()) {

            DatabaseMetaData meta = conn.getMetaData();
            Map<String, Table> tableMap = new HashMap<>();

            // ===============================
            // 1️⃣ Load Tables
            // ===============================
            try (ResultSet rsTables =
                         meta.getTables(null, null, "%", new String[]{"TABLE"})) {

                while (rsTables.next()) {

                    String tableName = rsTables.getString("TABLE_NAME");

                    Table table = new Table();
                    table.setName(tableName);
                    table.setColumns(new ArrayList<>());

                    tables.add(table);
                    tableMap.put(tableName, table);
                }
            }

            // ===============================
            // 2️⃣ Load Columns
            // ===============================
            for (Table table : tables) {

                try (ResultSet rsColumns =
                             meta.getColumns(null, null, table.getName(), null)) {

                    while (rsColumns.next()) {

                        String colName = rsColumns.getString("COLUMN_NAME");
                        String type = rsColumns.getString("TYPE_NAME");

                        table.getColumns().add(
                                new Column(colName, type, false)
                        );
                    }
                }
            }

            // ===============================
            // 3️⃣ Load Primary Keys
            // ===============================
            // Build fast PK index for inference
            Map<String, Set<String>> pkIndex = new HashMap<>();

            for (Table table : tables) {

                Set<String> pkCols = new HashSet<>();

                try (ResultSet rsPK =
                             meta.getPrimaryKeys(null, null, table.getName())) {

                    while (rsPK.next()) {

                        String pkCol = rsPK.getString("COLUMN_NAME");
                        pkCols.add(pkCol.toLowerCase());

                        for (Column c : table.getColumns()) {
                            if (c.getName().equalsIgnoreCase(pkCol)) {
                                c.setPrimaryKey(true);
                            }
                        }
                    }
                }

                pkIndex.put(table.getName(), pkCols);
            }

            // ===============================
            // 4️⃣ Load Foreign Keys (Strict)
            // ===============================
            for (Table table : tables) {

                try (ResultSet rsFK =
                             meta.getImportedKeys(null, null, table.getName())) {

                    while (rsFK.next()) {

                        String pkTable = rsFK.getString("PKTABLE_NAME");
                        String fkColumn = rsFK.getString("FKCOLUMN_NAME");
                        String pkColumn = rsFK.getString("PKCOLUMN_NAME");

                        relationships.add(
                                new Relationship(
                                        table.getName(),
                                        pkTable,
                                        fkColumn,
                                        pkColumn,
                                        "strict",
                                        1.0
                                )
                        );
                    }
                }
            }

            // ===============================
            // 5️⃣ Optimized Inferred Detection
            // O(totalColumns) instead of O(n²)
            // ===============================

            for (Table table : tables) {

                for (Column column : table.getColumns()) {

                    if (column.isPrimaryKey()) continue;

                    String colName = column.getName().toLowerCase();

                    for (Map.Entry<String, Set<String>> entry : pkIndex.entrySet()) {

                        String targetTable = entry.getKey();

                        if (targetTable.equalsIgnoreCase(table.getName()))
                            continue;

                        if (entry.getValue().contains(colName)) {

                            relationships.add(
                                    new Relationship(
                                            table.getName(),
                                            targetTable,
                                            column.getName(),
                                            column.getName(),
                                            "inferred",
                                            0.6
                                    )
                            );
                        }
                    }
                }
            }
        }

        return new Schema(tables, relationships);
    }
}