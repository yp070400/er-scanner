package com.yogesh.er_scanner.service;

import com.yogesh.er_scanner.db.*;
import com.yogesh.er_scanner.model.*;

import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@Service
public class DataSampleService {

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

        Map<String, Map<String, Set<String>>> sampled =
                new HashMap<>();

        try (Connection conn = dataSource.getConnection()) {

            DatabaseDialect dialect =
                    dialectFactory.getDialect(conn);

            for (String table : tables) {

                Map<String, Set<String>> columnValues =
                        new HashMap<>();

                String sql =
                        dialect.getSampleQuery(table, sampleSize);

                try (Statement st =
                             conn.createStatement();
                     ResultSet rs =
                             st.executeQuery(sql)) {

                    ResultSetMetaData meta =
                            rs.getMetaData();

                    int colCount =
                            meta.getColumnCount();

                    while (rs.next()) {

                        for (int i = 1;
                             i <= colCount;
                             i++) {

                            String col =
                                    meta.getColumnName(i);

                            String val =
                                    rs.getString(i);

                            if (val == null) continue;

                            columnValues
                                    .computeIfAbsent(
                                            col,
                                            k -> new HashSet<>())
                                    .add(val);
                        }
                    }
                }

                sampled.put(table, columnValues);
            }
        }

        return computeOverlap(sampled);
    }

    private List<Relationship> computeOverlap(
            Map<String,
                    Map<String, Set<String>>> sampled) {

        List<Relationship> relationships =
                new ArrayList<>();

        List<String> tables =
                new ArrayList<>(sampled.keySet());

        for (int i = 0; i < tables.size(); i++) {
            for (int j = i + 1; j < tables.size(); j++) {

                String t1 = tables.get(i);
                String t2 = tables.get(j);

                Map<String, Set<String>> c1 =
                        sampled.get(t1);
                Map<String, Set<String>> c2 =
                        sampled.get(t2);

                for (String col1 : c1.keySet()) {
                    for (String col2 : c2.keySet()) {

                        Set<String> s1 = c1.get(col1);
                        Set<String> s2 = c2.get(col2);

                        Set<String> intersection =
                                new HashSet<>(s1);
                        intersection.retainAll(s2);

                        double overlap =
                                (double) intersection.size()
                                        / Math.min(
                                        s1.size(),
                                        s2.size());

                        if (overlap >= 0.6) {

                            relationships.add(
                                    new Relationship(
                                            t1,
                                            col1,
                                            t2,
                                            col2,
                                            RelationshipType.DATA_SAMPLE,
                                            overlap
                                    )
                            );
                        }
                    }
                }
            }
        }

        return relationships;
    }
}