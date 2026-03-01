package com.yogesh.er_scanner.dto;

import java.util.List;

public class MermaidTableDTO {

    private String definition;
    private List<String> relationships;

    public MermaidTableDTO(String definition, List<String> relationships) {
        this.definition = definition;
        this.relationships = relationships;
    }

    public String getDefinition() {
        return definition;
    }

    public List<String> getRelationships() {
        return relationships;
    }
}