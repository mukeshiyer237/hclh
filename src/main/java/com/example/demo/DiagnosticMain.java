package com.example.demo;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;

import java.sql.Connection;
import java.sql.ResultSet;

/**
 * Diagnostic runner — NOT part of the application.
 * Run with: mvn exec:java -Dexec.mainClass=com.example.demo.DiagnosticMain
 * to independently verify DB + Flyway connectivity.
 */
public class DiagnosticMain {

    public static void main(String[] args) throws Exception {
        String url      = System.getenv("DB_URL");
        String user     = System.getenv("DB_USERNAME");
        String password = System.getenv("DB_PASSWORD");

        System.out.println("=== DB Connectivity ===");
        System.out.println("URL:  " + url);
        System.out.println("User: " + user);

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(user);
        cfg.setPassword(password);
        cfg.setConnectionTimeout(10_000);
        cfg.setMaximumPoolSize(2);

        try (HikariDataSource ds = new HikariDataSource(cfg)) {
            try (Connection conn = ds.getConnection()) {
                System.out.println("DB connection: OK");

                ResultSet rs = conn.createStatement()
                        .executeQuery("SELECT version()");
                rs.next();
                System.out.println("DB version: " + rs.getString(1));

                // Check what tables exist
                ResultSet tables = conn.getMetaData()
                        .getTables(null, "public", "%", new String[]{"TABLE"});
                System.out.println("Tables in public schema:");
                while (tables.next()) {
                    System.out.println("  - " + tables.getString("TABLE_NAME"));
                }

                // Check flyway_schema_history
                try {
                    ResultSet fsh = conn.createStatement()
                            .executeQuery("SELECT version, description, checksum, success FROM flyway_schema_history ORDER BY installed_rank");
                    System.out.println("Flyway history:");
                    while (fsh.next()) {
                        System.out.printf("  V%s (%s) checksum=%s success=%s%n",
                                fsh.getString(1), fsh.getString(2),
                                fsh.getString(3), fsh.getString(4));
                    }
                } catch (Exception e) {
                    System.out.println("No flyway_schema_history: " + e.getMessage());
                }

                // Check audit_log columns
                try {
                    ResultSet cols = conn.getMetaData()
                            .getColumns(null, "public", "audit_log", "%");
                    System.out.println("audit_log columns:");
                    while (cols.next()) {
                        System.out.println("  - " + cols.getString("COLUMN_NAME") + " " + cols.getString("TYPE_NAME"));
                    }
                } catch (Exception e) {
                    System.out.println("audit_log not found: " + e.getMessage());
                }
            }
        }

        System.out.println("\n=== Flyway Repair + Migrate ===");
        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(url, user, password)
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .load();
            flyway.repair();
            System.out.println("Flyway repair: OK");
            var result = flyway.migrate();
            System.out.println("Flyway migrate: " + result.migrationsExecuted + " migrations executed");
        } catch (Exception e) {
            System.err.println("Flyway error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
