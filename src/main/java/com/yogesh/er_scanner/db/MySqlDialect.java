package com.yogesh.er_scanner.db;

public class MySqlDialect implements DatabaseDialect {

    @Override
    public String getSampleQuery(String tableName, int sampleSize) {
        return "SELECT * FROM " + tableName +
                " LIMIT " + sampleSize;
    }
}