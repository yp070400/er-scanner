package com.yogesh.er_scanner;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.File;
import java.sql.*;
import java.util.*;

@Component
public class SchemaScanner {

    private final DataSource dataSource;

    @Value("${output.dir}")
    private String outputDir;

    public SchemaScanner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void scan() throws Exception {

        Map<String, TableInfo> tables = new HashMap<>();
        List<Relationship> relationships = new ArrayList<>();

        try (Connection conn = dataSource.getConnection()) {

            DatabaseMetaData meta = conn.getMetaData();

            ResultSet rsTables = meta.getTables(null, null, "%", new String[]{"TABLE"});

            while (rsTables.next()) {
                String tableName = rsTables.getString("TABLE_NAME");
                tables.put(tableName, new TableInfo(tableName));
            }

            // Columns + PK
            for (String table : tables.keySet()) {

                ResultSet rsCols = meta.getColumns(null, null, table, "%");
                while (rsCols.next()) {
                    tables.get(table).columns.add(rsCols.getString("COLUMN_NAME"));
                }

                ResultSet pk = meta.getPrimaryKeys(null, null, table);
                while (pk.next()) {
                    tables.get(table).primaryKeys.add(pk.getString("COLUMN_NAME"));
                }
            }

            // Explicit FK detection
            for (String table : tables.keySet()) {

                ResultSet fk = meta.getImportedKeys(null, null, table);

                while (fk.next()) {
                    relationships.add(new Relationship(
                            table,
                            fk.getString("FKCOLUMN_NAME"),
                            fk.getString("PKTABLE_NAME"),
                            1.0
                    ));
                }
            }

            // Implicit inference
            for (TableInfo t1 : tables.values()) {

                for (String col : t1.columns) {

                    if (col.endsWith("_id")) {

                        for (TableInfo t2 : tables.values()) {

                            if (!t1.name.equals(t2.name)
                                    && t2.primaryKeys.contains(col)) {

                                double confidence = computeOverlap(
                                        conn,
                                        t1.name,
                                        col,
                                        t2.name,
                                        col
                                );

                                if (confidence > 0.5) {
                                    relationships.add(
                                            new Relationship(
                                                    t1.name,
                                                    col,
                                                    t2.name,
                                                    confidence
                                            )
                                    );
                                }
                            }
                        }
                    }
                }
            }
        }

        generateMermaid(relationships);
        generateJson(tables, relationships);

        System.out.println("ER files generated in /" + outputDir);
    }

    private double computeOverlap(Connection conn,
                                  String t1,
                                  String c1,
                                  String t2,
                                  String c2) {

        try (Statement st = conn.createStatement()) {

            ResultSet rs1 = st.executeQuery(
                    "SELECT COUNT(DISTINCT " + c1 + ") FROM " + t1);
            rs1.next();
            int total = rs1.getInt(1);

            ResultSet rs2 = st.executeQuery(
                    "SELECT COUNT(DISTINCT a." + c1 + ") FROM " + t1 +
                            " a JOIN " + t2 +
                            " b ON a." + c1 + " = b." + c2);
            rs2.next();
            int matched = rs2.getInt(1);

            if (total == 0) return 0;

            return (double) matched / total;

        } catch (Exception e) {
            return 0;
        }
    }

    private void generateMermaid(List<Relationship> relationships) throws Exception {

        new File(outputDir).mkdirs();
        File file = new File(outputDir + "/er_diagram.mmd");

        StringBuilder sb = new StringBuilder();
        sb.append("erDiagram\n");

        for (Relationship r : relationships) {

            sb.append("    ")
                    .append(r.toTable.toUpperCase())
                    .append(" ||--o{ ")
                    .append(r.fromTable.toUpperCase())
                    .append(" : ")
                    .append(r.column)
                    .append("\n");
        }

        java.nio.file.Files.write(file.toPath(),
                sb.toString().getBytes());
        generateHtml(sb.toString());
    }

    private void generateHtml(String mermaidContent) throws Exception {

        File file = new File(outputDir + "/er_visualization.html");

        String html = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <script src="https://cdn.jsdelivr.net/npm/mermaid/dist/mermaid.min.js"></script>
        </head>
        <body>
        <h2>Database ER Diagram</h2>
        <div class="mermaid">
        """ + mermaidContent + """
        </div>
        <script>
            mermaid.initialize({ startOnLoad: true });
        </script>
        </body>
        </html>
        """;

        java.nio.file.Files.write(file.toPath(), html.getBytes());
    }

    private void generateJson(Map<String, TableInfo> tables,
                              List<Relationship> relationships) throws Exception {

        Map<String, Object> graph = new HashMap<>();

        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        // Nodes
        for (TableInfo table : tables.values()) {
            Map<String, Object> nodeData = new HashMap<>();
            nodeData.put("id", table.name);
            nodeData.put("label", table.name);
            nodeData.put("columns", table.columns);

            nodes.add(Map.of("data", nodeData));
        }

        // Edges
        for (Relationship rel : relationships) {

            Map<String, Object> edgeData = new HashMap<>();
            edgeData.put("id", rel.fromTable + "_" + rel.toTable + "_" + rel.column);
            edgeData.put("source", rel.fromTable);
            edgeData.put("target", rel.toTable);
            edgeData.put("column", rel.column);
            edgeData.put("confidence", rel.confidence);

            edges.add(Map.of("data", edgeData));
        }

        graph.put("nodes", nodes);
        graph.put("edges", edges);

        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(new File(outputDir + "/cytoscape_schema.json"), graph);
    }
}