package com.yogesh.er_scanner.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "relationship")
public class RelationshipConfig {

    private boolean enabled;
    private int sampleSize;
    private int maxTables;
    private List<String> tables;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getSampleSize() { return sampleSize; }
    public void setSampleSize(int sampleSize) { this.sampleSize = sampleSize; }

    public int getMaxTables() { return maxTables; }
    public void setMaxTables(int maxTables) { this.maxTables = maxTables; }

    public List<String> getTables() { return tables; }
    public void setTables(List<String> tables) { this.tables = tables; }
}