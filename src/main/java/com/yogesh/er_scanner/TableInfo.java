package com.yogesh.er_scanner;

import java.util.ArrayList;
import java.util.List;

public class TableInfo {

    public String name;
    public List<String> columns = new ArrayList<>();
    public List<String> primaryKeys = new ArrayList<>();

    public TableInfo(String name) {
        this.name = name;
    }
}