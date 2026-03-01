package com.yogesh.er_scanner.model;

public class Relationship {

    private String sourceTable;
    private String sourceColumn;
    private String targetTable;
    private String targetColumn;
    private RelationshipType relationshipType;
    private double confidence;

    public Relationship() {}

    public Relationship(String sourceTable,
                        String sourceColumn,
                        String targetTable,
                        String targetColumn,
                        RelationshipType relationshipType,
                        double confidence) {

        this.sourceTable = sourceTable;
        this.sourceColumn = sourceColumn;
        this.targetTable = targetTable;
        this.targetColumn = targetColumn;
        this.relationshipType = relationshipType;
        this.confidence = confidence;
    }

    public String getSourceTable() { return sourceTable; }
    public String getSourceColumn() { return sourceColumn; }
    public String getTargetTable() { return targetTable; }
    public String getTargetColumn() { return targetColumn; }
    public RelationshipType getRelationshipType() { return relationshipType; }
    public double getConfidence() { return confidence; }
}