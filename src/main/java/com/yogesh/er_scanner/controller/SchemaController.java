package com.yogesh.er_scanner.controller;

import com.yogesh.er_scanner.service.SchemaScanner;
import com.yogesh.er_scanner.service.SchemaService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/schema")
public class SchemaController {

    private final SchemaScanner scanner;
    private final SchemaService service;

    public SchemaController(SchemaScanner scanner, SchemaService service) {
        this.scanner = scanner;
        this.service = service;
    }

    @PostMapping("/scan")
    public String scan() throws Exception {

        service.setSchema(scanner.scan());
        service.writeAiJson();

        return "Schema scanned and files generated successfully.";
    }

    @GetMapping("/visual")
    public Map<String, Object> visual() {
        return service.generateVisualJson();
    }

    @GetMapping(value = "/er-mermaid", produces = "text/plain")
    public String mermaid() {
        return service.generateMermaid();
    }

    @GetMapping("/er-mermaid-chunks")
    public Map<String, String> mermaidChunks() {
        return service.generateMermaidChunks(40); // 40 tables per chunk
    }

}