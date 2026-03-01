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

                List<Column> columns = new ArrayList<>();

                ResultSet rsColumns = meta.getColumns(null, null, tableName, null);
                while (rsColumns.next()) {

                    String colName = rsColumns.getString("COLUMN_NAME");
                    String type = rsColumns.getString("TYPE_NAME");

                    columns.add(new Column(colName, type, false));
                }

                ResultSet rsPK = meta.getPrimaryKeys(null, null, tableName);
                while (rsPK.next()) {

                    String pkCol = rsPK.getString("COLUMN_NAME");

                    for (Column c : columns) {
                        if (c.getName().equalsIgnoreCase(pkCol)) {
                            c.setPrimaryKey(true);
                        }
                    }
                }

                Table table = new Table(tableName, columns);
                tables.add(table);
                tableMap.put(tableName, table);
            }

            // Strict FK detection
            for (Table table : tables) {

                ResultSet rsFK = meta.getImportedKeys(null, null, table.getName());

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

            // Inferred relationship (column name matches PK)
            for (Table t1 : tables) {
                for (Table t2 : tables) {

                    if (t1.getName().equalsIgnoreCase(t2.getName()))
                        continue;

                    for (Column c1 : t1.getColumns()) {

                        if (!c1.isPrimaryKey()) {

                            for (Column c2 : t2.getColumns()) {

                                if (c2.isPrimaryKey() &&
                                        c1.getName().equalsIgnoreCase(c2.getName())) {

                                    relationships.add(
                                            new Relationship(
                                                    t1.getName(),
                                                    t2.getName(),
                                                    c1.getName(),
                                                    c2.getName(),
                                                    "inferred",
                                                    0.6
                                            )
                                    );
                                }
                            }
                        }
                    }
                }
            }
        }

        return new Schema(tables, relationships);
    }
}