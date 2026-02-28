package com.yogesh.er_scanner;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/schema")
public class SchemaController {

    private final SchemaService schemaService;

    public SchemaController(SchemaService schemaService) {
        this.schemaService = schemaService;
    }

    @GetMapping
    public Object getSchema() throws Exception {
        return schemaService.getSchema();
    }

    @PostMapping("/scan")
    public String regenerate() throws Exception {
        schemaService.regenerateSchema();
        return "Schema regenerated successfully.";
    }
}