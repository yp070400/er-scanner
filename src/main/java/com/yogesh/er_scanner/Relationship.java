package com.yogesh.er_scanner;

public class Relationship {

    public String fromTable;
    public String column;
    public String toTable;
    public double confidence;

    public Relationship(String fromTable,
                        String column,
                        String toTable,
                        double confidence) {
        this.fromTable = fromTable;
        this.column = column;
        this.toTable = toTable;
        this.confidence = confidence;
    }
}