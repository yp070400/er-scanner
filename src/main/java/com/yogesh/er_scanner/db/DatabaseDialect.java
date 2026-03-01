package com.yogesh.er_scanner.db;

public interface DatabaseDialect {

    String getSampleQuery(String tableName, int sampleSize);
}