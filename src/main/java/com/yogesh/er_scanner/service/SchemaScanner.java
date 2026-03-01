package com.yogesh.er_scanner.service;

import com.yogesh.er_scanner.config.RelationshipConfig;
import com.yogesh.er_scanner.model.*;

import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SchemaScanner {

    private final DataSource dataSource;
    private final RelationshipConfig relationshipConfig;

    public SchemaScanner(DataSource dataSource,
                         RelationshipConfig relationshipConfig) {
        this.dataSource = dataSource;
        this.relationshipConfig = relationshipConfig;
    }

    public Schema scan() throws Exception {

        List<Table> tables = new ArrayList<>();
        List<Relationship> relationships = new ArrayList<>();

        if (!relationshipConfig.isEnabled()) {
            throw new RuntimeException(
                    "Relationship scanning disabled in config.");
        }

        List<String> configuredTables =
                relationshipConfig.getTables();

        if (configuredTables == null ||
                configuredTables.isEmpty()) {
            throw new RuntimeException(
                    "No tables configured for scanning.");
        }

        if (configuredTables.size() >
                relationshipConfig.getMaxTables()) {
            throw new RuntimeException(
                    "Too many tables configured.");
        }

        try (Connection conn = dataSource.getConnection()) {

            DatabaseMetaData meta = conn.getMetaData();

            Set<String> configuredSet =
                    configuredTables.stream()
                            .map(String::toLowerCase)
                            .collect(Collectors.toSet());

            for (String tableName : configuredTables) {

                List<Column> columns = new ArrayList<>();

                // ---------- LOAD COLUMNS ----------
                try (ResultSet rsColumns =
                             meta.getColumns(
                                     null,
                                     null,
                                     tableName,
                                     null)) {

                    while (rsColumns.next()) {

                        columns.add(
                                new Column(
                                        rsColumns.getString("COLUMN_NAME"),
                                        rsColumns.getString("TYPE_NAME"),
                                        false
                                )
                        );
                    }
                }

                // ---------- LOAD PRIMARY KEYS ----------
                try (ResultSet rsPK =
                             meta.getPrimaryKeys(
                                     null,
                                     null,
                                     tableName)) {

                    while (rsPK.next()) {

                        String pk =
                                rsPK.getString("COLUMN_NAME");

                        columns.stream()
                                .filter(c ->
                                        c.getName()
                                                .equalsIgnoreCase(pk))
                                .forEach(c ->
                                        c.setPrimaryKey(true));
                    }
                }

                // ---------- LOAD FOREIGN KEYS ----------
                try (ResultSet rsFK =
                             meta.getImportedKeys(
                                     null,
                                     null,
                                     tableName)) {

                    while (rsFK.next()) {

                        String referencedTable =
                                rsFK.getString("PKTABLE_NAME");

                        // Only allow relationships
                        // inside configured tables
                        if (!configuredSet.contains(
                                referencedTable.toLowerCase())) {
                            continue;
                        }

                        String fkColumn =
                                rsFK.getString("FKCOLUMN_NAME");

                        columns.stream()
                                .filter(c ->
                                        c.getName()
                                                .equalsIgnoreCase(fkColumn))
                                .forEach(c ->
                                        c.setForeignKey(true));

                        relationships.add(
                                new Relationship(
                                        tableName,
                                        fkColumn,
                                        referencedTable,
                                        rsFK.getString("PKCOLUMN_NAME"),
                                        RelationshipType.STRICT,
                                        1.0
                                )
                        );
                    }
                }

                tables.add(new Table(tableName, columns));
            }
        }

        return new Schema(tables, relationships);
    }
}