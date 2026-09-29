package com.manacommunity.sports.config.tenant;

import com.manacommunity.common.enums.*;
import com.manacommunity.common.model.Community;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility service for managing tenant schemas at runtime.
 * Used when onboarding a new community to create its isolated schema
 * and replicate the base table structure.
 */
@Service
public class TenantSchemaService {

    private static final Logger log = LoggerFactory.getLogger(TenantSchemaService.class);

    private final DataSource dataSource;

    public TenantSchemaService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Creates a new PostgreSQL schema for a tenant by cloning the structure
     * (tables, indexes, constraints) from the default schema.
     *
     * @param schemaName must match {@code ^[a-z_][a-z0-9_]*$}
     */
    public void createTenantSchema(String schemaName) {
        if (schemaName == null || !schemaName.matches("^[a-z_][a-z0-9_]*$")) {
            throw new IllegalArgumentException("Invalid schema name: " + schemaName);
        }

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);

            // Clone all tables from the default schema
            ResultSet tables = stmt.executeQuery(
                    "SELECT table_name FROM information_schema.tables " +
                    "WHERE table_schema = '" + TenantContext.DEFAULT_SCHEMA + "' " +
                    "AND table_type = 'BASE TABLE'"
            );

            List<String> tableNames = new ArrayList<>();
            while (tables.next()) {
                tableNames.add(tables.getString("table_name"));
            }

            for (String table : tableNames) {
                String createLike = String.format(
                        "CREATE TABLE IF NOT EXISTS %s.%s (LIKE %s.%s INCLUDING ALL)",
                        schemaName, table, TenantContext.DEFAULT_SCHEMA, table
                );
                stmt.execute(createLike);
            }

            log.info("Created tenant schema '{}' with {} tables", schemaName, tableNames.size());

        } catch (SQLException e) {
            log.error("Failed to create tenant schema '{}'", schemaName, e);
            throw new RuntimeException("Failed to create tenant schema: " + schemaName, e);
        }
    }

    /**
     * Lists all tenant schemas (excludes system schemas).
     */
    public List<String> listTenantSchemas() {
        List<String> schemas = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT schema_name FROM information_schema.schemata " +
                     "WHERE schema_name NOT IN ('pg_catalog', 'information_schema', 'pg_toast') " +
                     "ORDER BY schema_name"
             )) {
            while (rs.next()) {
                schemas.add(rs.getString("schema_name"));
            }
        } catch (SQLException e) {
            log.error("Failed to list tenant schemas", e);
        }
        return schemas;
    }
}


