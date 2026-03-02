package com.yogesh.er_scanner.service;

import com.yogesh.er_scanner.db.*;
import com.yogesh.er_scanner.model.*;

import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@Service
public class DataSampleService {

    private static final Set<String> SYSTEM_COLUMNS = Set.of(
            "USER", "CURRENT_CONNECTIONS", "TOTAL_CONNECTIONS",
            "MAX_SESSION_CONTROLLED_MEMORY", "MAX_SESSION_TOTAL_MEMORY"
    );

    // JDBC type groups for compatibility checks
    private static final Set<Integer> NUMERIC_TYPES = Set.of(
            Types.INTEGER, Types.BIGINT, Types.SMALLINT, Types.TINYINT,
            Types.FLOAT, Types.DOUBLE, Types.REAL, Types.DECIMAL, Types.NUMERIC
    );
    private static final Set<Integer> STRING_TYPES = Set.of(
            Types.VARCHAR, Types.CHAR, Types.LONGVARCHAR, Types.NVARCHAR,
            Types.NCHAR, Types.LONGNVARCHAR, Types.CLOB, Types.NCLOB
    );
    private static final Set<Integer> DATE_TYPES = Set.of(
            Types.DATE, Types.TIMESTAMP, Types.TIME, Types.TIMESTAMP_WITH_TIMEZONE
    );

    private final DataSource dataSource;
    private final DatabaseDialectFactory dialectFactory;

    public DataSampleService(DataSource dataSource,
                             DatabaseDialectFactory dialectFactory) {
        this.dataSource = dataSource;
        this.dialectFactory = dialectFactory;
    }

    public List<Relationship> detectSampleRelationships(
            List<String> tables,
            int sampleSize) throws Exception {

        // value sets per table/column
        Map<String, Map<String, Set<String>>> sampled = new HashMap<>();
        // SQL type per table/column
        Map<String, Map<String, Integer>> sqlTypes = new HashMap<>();

        try (Connection conn = dataSource.getConnection()) {

            DatabaseDialect dialect = dialectFactory.getDialect(conn);

            for (String table : tables) {

                Map<String, Set<String>> columnValues = new HashMap<>();
                Map<String, Integer> columnTypes = new HashMap<>();

                String sql = dialect.getSampleQuery(table, sampleSize);

                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery(sql)) {

                    ResultSetMetaData meta = rs.getMetaData();
                    int colCount = meta.getColumnCount();

                    // Collect type info and filter system columns before reading rows
                    List<Integer> usedIndexes = new ArrayList<>();
                    for (int i = 1; i <= colCount; i++) {
                        String col = meta.getColumnName(i);
                        if (SYSTEM_COLUMNS.contains(col.toUpperCase())) continue;
                        usedIndexes.add(i);
                        columnTypes.put(col, meta.getColumnType(i));
                    }

                    while (rs.next()) {
                        for (int i : usedIndexes) {
                            String col = meta.getColumnName(i);
                            String val = rs.getString(i);
                            if (val == null) continue;
                            columnValues
                                    .computeIfAbsent(col, k -> new HashSet<>())
                                    .add(val);
                        }
                    }
                }

                // Drop cardinality-1 columns — single unique value means no discriminating power
                columnValues.entrySet().removeIf(e -> e.getValue().size() <= 1);
                // Keep only types for columns that survived the cardinality filter
                columnTypes.keySet().retainAll(columnValues.keySet());

                sampled.put(table, columnValues);
                sqlTypes.put(table, columnTypes);
            }
        }

        return computeOverlap(sampled, sqlTypes);
    }

    private List<Relationship> computeOverlap(
            Map<String, Map<String, Set<String>>> sampled,
            Map<String, Map<String, Integer>> sqlTypes) {

        List<Relationship> relationships = new ArrayList<>();
        List<String> tables = new ArrayList<>(sampled.keySet());

        for (int i = 0; i < tables.size(); i++) {
            for (int j = i + 1; j < tables.size(); j++) {

                String t1 = tables.get(i);
                String t2 = tables.get(j);

                Map<String, Set<String>> c1 = sampled.get(t1);
                Map<String, Set<String>> c2 = sampled.get(t2);
                Map<String, Integer> types1 = sqlTypes.get(t1);
                Map<String, Integer> types2 = sqlTypes.get(t2);

                for (String col1 : c1.keySet()) {
                    for (String col2 : c2.keySet()) {

                        // Type compatibility check
                        Integer type1 = types1.get(col1);
                        Integer type2 = types2.get(col2);
                        if (type1 != null && type2 != null && !typesCompatible(type1, type2)) {
                            continue;
                        }

                        Set<String> s1 = c1.get(col1);
                        Set<String> s2 = c2.get(col2);

                        Set<String> intersection = new HashSet<>(s1);
                        intersection.retainAll(s2);

                        double overlap = (double) intersection.size()
                                / Math.min(s1.size(), s2.size());

                        if (overlap >= 0.9) {
                            relationships.add(new Relationship(
                                    t1, col1, t2, col2,
                                    RelationshipType.DATA_INFERRED, overlap));
                        } else if (overlap >= 0.6) {
                            relationships.add(new Relationship(
                                    t1, col1, t2, col2,
                                    RelationshipType.DATA_SAMPLE, overlap));
                        }
                    }
                }
            }
        }

        return relationships;
    }

    private boolean typesCompatible(int t1, int t2) {
        return (NUMERIC_TYPES.contains(t1) && NUMERIC_TYPES.contains(t2))
                || (STRING_TYPES.contains(t1) && STRING_TYPES.contains(t2))
                || (DATE_TYPES.contains(t1) && DATE_TYPES.contains(t2));
    }
}
