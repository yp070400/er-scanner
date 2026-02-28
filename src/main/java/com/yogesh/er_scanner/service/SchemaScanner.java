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
            ResultSet rsTables = meta.getTables(null, null, "%", new String[]{"TABLE"});

            Map<String, Table> tableMap = new HashMap<>();

            while (rsTables.next()) {

                String tableName = rsTables.getString("TABLE_NAME");

                List<String> columns = new ArrayList<>();
                List<String> primaryKeys = new ArrayList<>();

                ResultSet rsColumns = meta.getColumns(null, null, tableName, null);
                while (rsColumns.next()) {
                    columns.add(rsColumns.getString("COLUMN_NAME"));
                }

                ResultSet rsPK = meta.getPrimaryKeys(null, null, tableName);
                while (rsPK.next()) {
                    primaryKeys.add(rsPK.getString("COLUMN_NAME"));
                }

                Table table = new Table(tableName, columns, primaryKeys);
                tables.add(table);
                tableMap.put(tableName, table);

                // STRICT FK detection
                ResultSet rsFK = meta.getImportedKeys(null, null, tableName);
                while (rsFK.next()) {

                    String pkTable = rsFK.getString("PKTABLE_NAME");
                    String fkColumn = rsFK.getString("FKCOLUMN_NAME");

                    relationships.add(
                            new Relationship(
                                    tableName,
                                    pkTable,
                                    fkColumn,
                                    "strict",
                                    1.0
                            )
                    );
                }
            }

            // INFERRED relationships (name match)
            for (Table t1 : tables) {
                for (Table t2 : tables) {

                    if (t1 == t2) continue;

                    for (String col1 : t1.getColumns()) {
                        if (t2.getPrimaryKeys().contains(col1)) {

                            relationships.add(
                                    new Relationship(
                                            t1.getName(),
                                            t2.getName(),
                                            col1,
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