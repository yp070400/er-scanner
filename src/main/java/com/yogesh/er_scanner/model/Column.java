package com.yogesh.er_scanner.model;

public class Column {

    private String name;
    private String type;
    private boolean nullable;
    private ForeignKey foreignKey;

    public Column(String name, String type, boolean nullable) {
        this.name = name;
        this.type = type;
        this.nullable = nullable;
    }

    public String getName() { return name; }
    public String getType() { return type; }
    public boolean isNullable() { return nullable; }

    public ForeignKey getForeignKey() { return foreignKey; }
    public void setForeignKey(ForeignKey foreignKey) {
        this.foreignKey = foreignKey;
    }
}