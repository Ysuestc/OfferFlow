package io.github.ysuestc.offerflow.persistence;

import java.net.URI;
import java.util.Properties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

record MySqlTestSettings(Database primary, Database migration) {

    static MySqlTestSettings fromEnvironment() {
        String username = required("OFFERFLOW_TEST_DB_USERNAME");
        String password = required("OFFERFLOW_TEST_DB_PASSWORD");
        Database primary = database("OFFERFLOW_TEST_DB_URL", username, password);
        Database migration = database("OFFERFLOW_TEST_MIGRATION_DB_URL", username, password);
        if (primary.url().equals(migration.url())) {
            throw new IllegalStateException("Integration tests require two distinct dedicated schemas");
        }
        return new MySqlTestSettings(primary, migration);
    }

    static Database workspace() {
        MySqlTestSettings settings = fromEnvironment();
        Database database = database("OFFERFLOW_TEST_WORKSPACE_DB_URL",
                required("OFFERFLOW_TEST_DB_USERNAME"), required("OFFERFLOW_TEST_DB_PASSWORD"));
        if (database.url().equals(settings.primary().url()) || database.url().equals(settings.migration().url())) {
            throw new IllegalStateException("Workspace tests require a third distinct dedicated schema");
        }
        return database;
    }

    private static String required(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required integration setting: " + key);
        }
        return value;
    }

    private static Database database(String key, String username, String password) {
        String url = required(key);
        URI uri;
        try {
            if (!url.startsWith("jdbc:mysql://")) {
                throw new IllegalArgumentException();
            }
            uri = URI.create(url.substring("jdbc:".length()));
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(key + " must be a plain dedicated MySQL JDBC URL");
        }
        if (uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null
                || uri.getFragment() != null || uri.getPath() == null
                || !uri.getPath().matches("/offerflow_it_[a-zA-Z0-9_]+")
                || uri.getPath().length() > 65) {
            throw new IllegalStateException(key + " must target an offerflow_it_ schema without URL credentials or parameters");
        }
        return new Database(url, username, password);
    }

    record Database(String url, String username, String password) {

        DriverManagerDataSource dataSource() {
            DriverManagerDataSource source = new DriverManagerDataSource(url, username, password);
            Properties properties = new Properties();
            properties.setProperty("connectionTimeZone", "UTC");
            properties.setProperty("forceConnectionTimeZoneToSession", "true");
            properties.setProperty("preserveInstants", "true");
            properties.setProperty("characterEncoding", "UTF-8");
            source.setConnectionProperties(properties);
            return source;
        }

        void requireEmptySchema() {
            Integer tables = new JdbcTemplate(dataSource()).queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()",
                    Integer.class);
            if (tables == null || tables != 0) {
                throw new IllegalStateException("Integration tests require a new, empty dedicated schema");
            }
        }

        // Never include the generated password in diagnostics or record toString.
        @Override
        public String toString() {
            return "DedicatedMySqlTestDatabase";
        }
    }
}
