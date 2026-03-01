package com.yogesh.er_scanner.model;

import java.util.List;

public class Schema {

    private List<Table> tables;
    private List<Relationship> relationships;

    public Schema() {}

    public Schema(List<Table> tables,
                  List<Relationship> relationships) {
        this.tables = tables;
        this.relationships = relationships;
    }

    public List<Table> getTables() { return tables; }
    public List<Relationship> getRelationships() { return relationships; }
}