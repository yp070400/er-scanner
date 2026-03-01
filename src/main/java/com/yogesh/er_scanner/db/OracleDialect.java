package com.yogesh.er_scanner.db;

public class OracleDialect implements DatabaseDialect {

    @Override
    public String getSampleQuery(String tableName, int sampleSize) {
        return "SELECT * FROM " + tableName +
                " WHERE ROWNUM <= " + sampleSize;
    }
}