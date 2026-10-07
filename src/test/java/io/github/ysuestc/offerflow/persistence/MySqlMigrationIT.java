package io.github.ysuestc.offerflow.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.exception.FlywayValidateException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class MySqlMigrationIT {

    @Test
    void migratesOnceRetainsDataAndRejectsBaselineChecksumMismatchAndClean() {
        var database = MySqlTestSettings.fromEnvironment().migration();
        database.requireEmptySchema();
        JdbcTemplate jdbc = new JdbcTemplate(database.dataSource());
        Flyway flyway = Flyway.configure()
                .dataSource(database.dataSource())
                .locations("classpath:db/migration")
                .baselineOnMigrate(false)
                .cleanDisabled(true)
                .load();

        jdbc.execute("CREATE TABLE unmanaged_marker (id INT PRIMARY KEY)");
        assertThatThrownBy(flyway::migrate).isInstanceOf(FlywayException.class)
                .hasMessageContaining("non-empty");
        jdbc.execute("DROP TABLE unmanaged_marker");

        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("1");
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = DATABASE() AND table_name <> 'flyway_schema_history'
                AND engine = 'InnoDB'
                """, Integer.class)).isEqualTo(6);
        assertThat(jdbc.queryForList("SHOW TABLES", String.class)).contains(
                "company", "job_position", "application", "application_stage_history", "interview", "todo");

        jdbc.update("INSERT INTO company (name) VALUES (?)", "迁移保留样例");
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForObject("SELECT name FROM company", String.class)).isEqualTo("迁移保留样例");
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1' AND success = 1",
                Integer.class)).isEqualTo(1);

        Integer checksum = jdbc.queryForObject(
                "SELECT checksum FROM flyway_schema_history WHERE version = '1'", Integer.class);
        try {
            jdbc.update("UPDATE flyway_schema_history SET checksum = checksum + 1 WHERE version = '1'");
            assertThatThrownBy(flyway::validate).isInstanceOf(FlywayValidateException.class);
            assertThatThrownBy(flyway::migrate).isInstanceOf(FlywayValidateException.class);
        } finally {
            jdbc.update("UPDATE flyway_schema_history SET checksum = ? WHERE version = '1'", checksum);
        }
        flyway.validate();
        assertThatThrownBy(flyway::clean).isInstanceOf(FlywayException.class)
                .hasMessageContaining("cleanDisabled");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM company", Integer.class)).isEqualTo(1);
    }
}
