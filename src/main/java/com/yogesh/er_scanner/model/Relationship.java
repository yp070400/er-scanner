package com.yogesh.er_scanner.model;

public class Relationship {

    private String source;
    private String target;
    private String column;
    private String type;        // strict | inferred
    private double confidence;  // 1.0 strict, 0.6 inferred

    public Relationship(String source, String target,
                        String column, String type, double confidence) {
        this.source = source;
        this.target = target;
        this.column = column;
        this.type = type;
        this.confidence = confidence;
    }

    public String getSource() { return source; }
    public String getTarget() { return target; }
    public String getColumn() { return column; }
    public String getType() { return type; }
    public double getConfidence() { return confidence; }
}