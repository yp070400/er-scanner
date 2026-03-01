package com.yogesh.er_scanner.model;

public class Relationship {

    private String sourceTable;
    private String targetTable;

    private String sourceColumn;
    private String targetColumn;

    private String type;       // strict | inferred
    private double confidence; // 1.0 strict, 0.6 inferred

    public Relationship() {}

    public Relationship(String sourceTable,
                        String targetTable,
                        String sourceColumn,
                        String targetColumn,
                        String type,
                        double confidence) {

        this.sourceTable = sourceTable;
        this.targetTable = targetTable;
        this.sourceColumn = sourceColumn;
        this.targetColumn = targetColumn;
        this.type = type;
        this.confidence = confidence;
    }

    public String getSourceTable() {
        return sourceTable;
    }

    public void setSourceTable(String sourceTable) {
        this.sourceTable = sourceTable;
    }

    public String getTargetTable() {
        return targetTable;
    }

    public void setTargetTable(String targetTable) {
        this.targetTable = targetTable;
    }

    public String getSourceColumn() {
        return sourceColumn;
    }

    public void setSourceColumn(String sourceColumn) {
        this.sourceColumn = sourceColumn;
    }

    public String getTargetColumn() {
        return targetColumn;
    }

    public void setTargetColumn(String targetColumn) {
        this.targetColumn = targetColumn;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }
}