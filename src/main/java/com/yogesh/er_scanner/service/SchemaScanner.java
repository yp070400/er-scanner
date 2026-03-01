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

        long totalStart = System.currentTimeMillis();
        System.out.println("=== SCHEMA SCAN STARTED ===");

        List<Table> tables = new ArrayList<>();
        List<Relationship> relationships = new ArrayList<>();

        try (Connection conn = dataSource.getConnection()) {

            conn.setAutoCommit(false);

            Map<String, Table> tableMap = new HashMap<>();

            // ============================================================
            // 1️⃣ LOAD TABLES
            // ============================================================

            long start = System.currentTimeMillis();

            String tableSql = """
                    SELECT table_name
                    FROM all_tables
                    WHERE owner = USER
                      AND table_name NOT LIKE 'BIN$%'
                    """;

            try (PreparedStatement ps = conn.prepareStatement(tableSql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    String tableName = rs.getString("table_name");

                    Table table = new Table();
                    table.setName(tableName);
                    table.setColumns(new ArrayList<>());

                    tables.add(table);
                    tableMap.put(tableName, table);
                }
            }

            System.out.println("Loaded tables: " + tables.size()
                    + " in " + (System.currentTimeMillis() - start) + " ms");

            // ============================================================
            // 2️⃣ LOAD ALL COLUMNS
            // ============================================================

            start = System.currentTimeMillis();

            String columnSql = """
                    SELECT table_name, column_name, data_type
                    FROM all_tab_columns
                    WHERE owner = USER
                    """;

            int totalColumns = 0;

            try (PreparedStatement ps = conn.prepareStatement(columnSql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    String tableName = rs.getString("table_name");
                    String columnName = rs.getString("column_name");
                    String dataType = rs.getString("data_type");

                    Table table = tableMap.get(tableName);

                    if (table != null) {
                        table.getColumns().add(
                                new Column(columnName, dataType, false)
                        );
                        totalColumns++;
                    }
                }
            }

            System.out.println("Loaded columns: " + totalColumns
                    + " in " + (System.currentTimeMillis() - start) + " ms");

            // ============================================================
            // 3️⃣ LOAD PRIMARY KEYS
            // ============================================================

            start = System.currentTimeMillis();

            String pkSql = """
                    SELECT acc.table_name, acc.column_name
                    FROM all_constraints ac
                    JOIN all_cons_columns acc
                      ON ac.constraint_name = acc.constraint_name
                     AND ac.owner = acc.owner
                    WHERE ac.constraint_type = 'P'
                      AND ac.owner = USER
                    """;

            try (PreparedStatement ps = conn.prepareStatement(pkSql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    String tableName = rs.getString("table_name");
                    String columnName = rs.getString("column_name");

                    Table table = tableMap.get(tableName);

                    if (table != null) {
                        for (Column column : table.getColumns()) {
                            if (column.getName().equalsIgnoreCase(columnName)) {
                                column.setPrimaryKey(true);
                            }
                        }
                    }
                }
            }

            System.out.println("Primary keys loaded in "
                    + (System.currentTimeMillis() - start) + " ms");

            // ============================================================
            // 4️⃣ LOAD FOREIGN KEYS
            // ============================================================

            start = System.currentTimeMillis();

            String fkSql = """
                    SELECT
                        a.table_name source_table,
                        a.column_name source_column,
                        c.table_name target_table,
                        c.column_name target_column
                    FROM all_constraints fk
                    JOIN all_cons_columns a
                      ON fk.constraint_name = a.constraint_name
                     AND fk.owner = a.owner
                    JOIN all_constraints pk
                      ON fk.r_constraint_name = pk.constraint_name
                     AND fk.owner = pk.owner
                    JOIN all_cons_columns c
                      ON pk.constraint_name = c.constraint_name
                     AND pk.owner = c.owner
                    WHERE fk.constraint_type = 'R'
                      AND fk.owner = USER
                    """;

            int strictCount = 0;

            try (PreparedStatement ps = conn.prepareStatement(fkSql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    relationships.add(
                            new Relationship(
                                    rs.getString("source_table"),
                                    rs.getString("target_table"),
                                    rs.getString("source_column"),
                                    rs.getString("target_column"),
                                    "strict",
                                    1.0
                            )
                    );

                    strictCount++;
                }
            }

            System.out.println("Strict FKs loaded: " + strictCount
                    + " in " + (System.currentTimeMillis() - start) + " ms");

            // ============================================================
            // 5️⃣ ULTRA-OPTIMIZED INFERRED RELATIONSHIPS
            // ============================================================

            start = System.currentTimeMillis();

            Map<String, List<String>> pkReverseIndex = new HashMap<>();

            for (Table table : tables) {
                for (Column col : table.getColumns()) {
                    if (col.isPrimaryKey()) {
                        pkReverseIndex
                                .computeIfAbsent(
                                        col.getName().toLowerCase(),
                                        k -> new ArrayList<>())
                                .add(table.getName());
                    }
                }
            }

            int inferredCount = 0;

            for (Table table : tables) {

                for (Column column : table.getColumns()) {

                    if (column.isPrimaryKey()) continue;

                    List<String> targets =
                            pkReverseIndex.get(column.getName().toLowerCase());

                    if (targets != null) {

                        for (String targetTable : targets) {

                            if (!targetTable.equalsIgnoreCase(table.getName())) {

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

                                inferredCount++;
                            }
                        }
                    }
                }
            }

            System.out.println("Inferred relationships: " + inferredCount
                    + " in " + (System.currentTimeMillis() - start) + " ms");
        }

        System.out.println("=== SCHEMA SCAN COMPLETED ===");
        System.out.println("Total time: "
                + (System.currentTimeMillis() - totalStart) + " ms");
        System.out.println("================================");

        return new Schema(tables, relationships);
    }
}