package com.yogesh.er_scanner.model;

public class ForeignKey {

    private String referencedTable;
    private String referencedColumn;

    public ForeignKey(String referencedTable, String referencedColumn) {
        this.referencedTable = referencedTable;
        this.referencedColumn = referencedColumn;
    }

    public String getReferencedTable() { return referencedTable; }
    public String getReferencedColumn() { return referencedColumn; }
}