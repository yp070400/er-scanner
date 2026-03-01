package com.yogesh.er_scanner.db;

import org.springframework.stereotype.Component;
import java.sql.Connection;

@Component
public class DatabaseDialectFactory {

    public DatabaseDialect getDialect(Connection connection) throws Exception {

        String product =
                connection.getMetaData()
                        .getDatabaseProductName()
                        .toLowerCase();

        if (product.contains("oracle")) {
            return new OracleDialect();
        }

        if (product.contains("mysql")) {
            return new MySqlDialect();
        }

        throw new RuntimeException("Unsupported DB: " + product);
    }
}