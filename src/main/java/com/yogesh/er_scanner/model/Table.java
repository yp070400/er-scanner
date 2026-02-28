package com.yogesh.er_scanner.model;

import java.util.List;

public class Table {

    private String name;
    private List<String> columns;
    private List<String> primaryKeys;

    public Table(String name, List<String> columns, List<String> primaryKeys) {
        this.name = name;
        this.columns = columns;
        this.primaryKeys = primaryKeys;
    }

    public String getName() { return name; }
    public List<String> getColumns() { return columns; }
    public List<String> getPrimaryKeys() { return primaryKeys; }
}