package com.alpha.lanim.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

public final class Database {

    private Database() {
    }

    public static void init() {

        Path dataDir = Paths.get("data");
        try {
            if (Files.notExists(dataDir)) {
                Files.createDirectories(dataDir);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create data directory", e);
        }

        String schemaSql = loadSchemaSql();
        if (schemaSql != null && !schemaSql.isBlank()) {
            try (Connection conn = connect();
                 Statement stmt = conn.createStatement()) {
                for (String sql : schemaSql.split(";")) {
                    String trimmed = sql.trim();
                    if (!trimmed.isEmpty()) {
                        stmt.execute(trimmed);
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to execute schema.sql", e);
            }
        }
    }

    public static Connection connect() throws SQLException {
        String url = "jdbc:sqlite:data/lanim.db";
        return DriverManager.getConnection(url);
    }

    private static String loadSchemaSql() {
        try (InputStream is = Database.class.getResourceAsStream("/sql/schema.sql")) {
            if (is == null) {
                return null;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read schema.sql", e);
        }
    }
}