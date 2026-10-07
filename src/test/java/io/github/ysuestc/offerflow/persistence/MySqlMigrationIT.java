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

        Flyway v1 = Flyway.configure().dataSource(database.dataSource()).locations("classpath:db/migration")
                .target("1").baselineOnMigrate(false).cleanDisabled(true).load();
        assertThat(v1.migrate().migrationsExecuted).isEqualTo(1);
        jdbc.update("INSERT INTO company (id, name) VALUES (1, ?)", "迁移保留样例");
        jdbc.update("INSERT INTO job_position (id, company_id, name) VALUES (1, 1, '升级样例岗位')");
        jdbc.update("INSERT INTO application (id, job_position_id) VALUES (1, 1)");
        jdbc.update("INSERT INTO interview (id, application_id, round_name, review) VALUES (1, 1, '一面', '原有复盘')");
        jdbc.update("INSERT INTO todo (application_id, interview_id, kind, title) VALUES (1, 1, 'INTERVIEW', '原有事项')");
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("2");
        assertThat(jdbc.queryForObject("SELECT review FROM interview WHERE id = 1", String.class)).isEqualTo("原有复盘");
        assertThat(jdbc.queryForObject("SELECT title FROM todo WHERE interview_id = 1", String.class)).isEqualTo("原有事项");
        assertThat(jdbc.queryForObject("SELECT version FROM interview WHERE id = 1", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT version FROM todo WHERE interview_id = 1", Integer.class)).isZero();
        assertThatThrownBy(() -> jdbc.update("UPDATE interview SET version = -1 WHERE id = 1"))
                .rootCause().isInstanceOf(java.sql.SQLException.class).extracting("errorCode").isEqualTo(3819);
        assertThatThrownBy(() -> jdbc.update("UPDATE todo SET version = -1 WHERE interview_id = 1"))
                .rootCause().isInstanceOf(java.sql.SQLException.class).extracting("errorCode").isEqualTo(3819);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = DATABASE() AND table_name <> 'flyway_schema_history'
                AND engine = 'InnoDB'
                """, Integer.class)).isEqualTo(6);
        assertThat(jdbc.queryForList("SHOW TABLES", String.class)).contains(
                "company", "job_position", "application", "application_stage_history", "interview", "todo");

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
