package io.github.ysuestc.offerflow.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.ysuestc.offerflow.application.entity.Application;
import io.github.ysuestc.offerflow.application.entity.ApplicationStage;
import io.github.ysuestc.offerflow.application.entity.ApplicationStageHistory;
import io.github.ysuestc.offerflow.application.entity.EndReason;
import io.github.ysuestc.offerflow.application.mapper.ApplicationMapper;
import io.github.ysuestc.offerflow.application.mapper.ApplicationStageHistoryMapper;
import io.github.ysuestc.offerflow.common.persistence.BaseEntity;
import io.github.ysuestc.offerflow.company.entity.Company;
import io.github.ysuestc.offerflow.company.entity.CompanyType;
import io.github.ysuestc.offerflow.company.mapper.CompanyMapper;
import io.github.ysuestc.offerflow.interview.entity.Interview;
import io.github.ysuestc.offerflow.interview.entity.InterviewFormat;
import io.github.ysuestc.offerflow.interview.mapper.InterviewMapper;
import io.github.ysuestc.offerflow.position.entity.JobPosition;
import io.github.ysuestc.offerflow.position.mapper.JobPositionMapper;
import io.github.ysuestc.offerflow.todo.entity.Todo;
import io.github.ysuestc.offerflow.todo.entity.TodoKind;
import io.github.ysuestc.offerflow.todo.mapper.TodoMapper;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("mysql")
@Transactional
class MySqlPersistenceIT {

    private static final LocalDate SUBMISSION_DATE = LocalDate.of(2026, 9, 20);
    private static final Instant INTERVIEW_TIME = Instant.parse("2026-10-07T02:30:00.123456Z");

    @Autowired CompanyMapper companies;
    @Autowired JobPositionMapper positions;
    @Autowired ApplicationMapper applications;
    @Autowired ApplicationStageHistoryMapper histories;
    @Autowired InterviewMapper interviews;
    @Autowired TodoMapper todos;
    @Autowired JdbcTemplate jdbc;
    @Autowired SqlSessionTemplate sessions;
    @Autowired PlatformTransactionManager transactionManager;

    @DynamicPropertySource
    static void dedicatedDatabase(DynamicPropertyRegistry properties) {
        var database = MySqlTestSettings.fromEnvironment().primary();
        database.requireEmptySchema();
        properties.add("spring.datasource.url", database::url);
        properties.add("spring.datasource.username", database::username);
        properties.add("spring.datasource.password", database::password);
    }

    @Test
    void roundTripsAllSixMappersAndPreservesDatabaseAuditTimes() {
        Fixture f = fixture();
        Company company = companies.selectById(f.company().getId());
        JobPosition position = positions.selectById(f.position().getId());
        Application application = applications.selectById(f.application().getId());
        ApplicationStageHistory history = histories.selectById(f.history().getId());
        Interview interview = interviews.selectById(f.interview().getId());
        Todo todo = todos.selectById(f.todo().getId());

        assertThat(company.getName()).isEqualTo("星河科技🌟");
        assertThat(company.getType()).isEqualTo(CompanyType.INTERNET);
        assertThat(company.getWebsite()).isEqualTo("https://example.org");
        assertThat(position.getCompanyId()).isEqualTo(company.getId());
        assertThat(position.getJd()).isEqualTo("Java / Agent 开发🌟");
        assertThat(position.getDirection()).isEqualTo("Java 后端");
        assertThat(application.getJobPositionId()).isEqualTo(position.getId());
        assertThat(application.getChannel()).isEqualTo("官网");
        assertThat(application.getAppliedOn()).isEqualTo(SUBMISSION_DATE);
        assertThat(application.getSubmitted()).isTrue();
        assertThat(application.getCurrentStage()).isEqualTo(ApplicationStage.SUBMITTED);
        assertThat(application.getVersion()).isZero();
        assertThat(history.getApplicationId()).isEqualTo(application.getId());
        assertThat(history.getStage()).isEqualTo(ApplicationStage.SUBMITTED);
        assertThat(history.getStageOn()).isEqualTo(SUBMISSION_DATE);
        assertThat(interview.getFormat()).isEqualTo(InterviewFormat.ONLINE);
        assertThat(interview.getRoundName()).isEqualTo("一面");
        assertThat(interview.getQuestions()).isEqualTo("事务和并发🌟");
        assertThat(interview.getAnswers()).isEqualTo("隔离级别 / 乐观锁");
        assertThat(interview.getReview()).isEqualTo("复习 Spring 事务");
        assertThat(interview.getResult()).isEqualTo("待反馈");
        assertThat(todo.getKind()).isEqualTo(TodoKind.INTERVIEW);
        assertThat(todo.getInterviewId()).isEqualTo(interview.getId());
        assertThat(todo.getCompleted()).isFalse();
        assertThat(todo.getCompletedAt()).isNull();
        for (BaseEntity entity : List.of(company, position, application, history, interview, todo)) {
            assertThat(entity.getId()).isPositive();
            assertThat(entity.getCreatedAt()).isNotNull().isNotEqualTo(Instant.EPOCH);
            assertThat(entity.getUpdatedAt()).isNotNull().isNotEqualTo(Instant.EPOCH);
        }

        Instant created = company.getCreatedAt();
        Instant updated = company.getUpdatedAt();
        company.setName("修改后的虚构公司");
        company.setCreatedAt(Instant.EPOCH);
        company.setUpdatedAt(Instant.EPOCH);
        assertThat(companies.updateById(company)).isEqualTo(1);
        Company changed = companies.selectById(company.getId());
        assertThat(changed.getCreatedAt()).isEqualTo(created);
        assertThat(changed.getUpdatedAt()).isAfterOrEqualTo(updated).isNotEqualTo(Instant.EPOCH);
    }

    @Test
    void preservesUnknownBusinessDatesWithoutInventingRecordingDates() {
        Fixture f = fixture();
        JobPosition position = newPosition(f.company().getId(), "日期未知批次");
        Application application = new Application();
        application.setJobPositionId(position.getId());
        application.setSubmitted(true);
        application.setCurrentStage(ApplicationStage.SUBMITTED);
        applications.insert(application);
        ApplicationStageHistory history = history(application.getId(), ApplicationStage.SUBMITTED, null);
        histories.insert(history);
        Interview interview = new Interview();
        interview.setApplicationId(application.getId());
        interview.setRoundName("历史面试");
        interviews.insert(interview);
        Todo todo = new Todo();
        todo.setApplicationId(application.getId());
        todo.setKind(TodoKind.MATERIAL);
        todo.setTitle("待确认材料截止日期");
        todos.insert(todo);

        Application loaded = applications.selectById(application.getId());
        assertThat(loaded.getSubmitted()).isTrue();
        assertThat(loaded.getAppliedOn()).isNull();
        assertThat(loaded.getCurrentStageOn()).isNull();
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(histories.selectById(history.getId()).getStageOn()).isNull();
        assertThat(interviews.selectById(interview.getId()).getInterviewAt()).isNull();
        assertThat(interviews.selectById(interview.getId()).getFormat()).isNull();
        assertThat(todos.selectById(todo.getId()).getDueAt()).isNull();
    }

    @Test
    void rejectsDuplicateApplicationButAllowsAnotherRecruitmentBatch() {
        Fixture f = fixture();
        Application duplicate = new Application();
        duplicate.setJobPositionId(f.position().getId());
        duplicate.setChannel("内推");
        assertDatabaseError(1062, () -> applications.insert(duplicate));
        JobPosition secondBatch = newPosition(f.company().getId(), "2027 春招");
        assertThat(secondBatch.getName()).isEqualTo(f.position().getName());
        Application next = new Application();
        next.setJobPositionId(secondBatch.getId());
        assertThat(applications.insert(next)).isEqualTo(1);
        assertThat(applications.selectById(next.getId()).getCurrentStage()).isEqualTo(ApplicationStage.TO_APPLY);
    }

    @ParameterizedTest
    @MethodSource("orphanWrites")
    void rejectsOrphanChildren(String sql) {
        assertDatabaseError(1452, () -> jdbc.update(sql));
    }

    static Stream<String> orphanWrites() {
        return Stream.of(
                "INSERT INTO job_position(company_id, name) VALUES(-1, '虚构岗位')",
                "INSERT INTO application(job_position_id) VALUES(-1)",
                "INSERT INTO application_stage_history(application_id, stage) VALUES(-1, 'SUBMITTED')",
                "INSERT INTO interview(application_id, round_name) VALUES(-1, '一面')",
                "INSERT INTO todo(application_id, kind, title) VALUES(-1, 'MATERIAL', '材料')");
    }

    @Test
    void restrictsParentDeletionAndKeepsExistingChildren() {
        Fixture f = fixture();
        assertDatabaseError(1451, () -> companies.deleteById(f.company().getId()));
        assertDatabaseError(1451, () -> positions.deleteById(f.position().getId()));
        assertDatabaseError(1451, () -> applications.deleteById(f.application().getId()));
        assertDatabaseError(1451, () -> interviews.deleteById(f.interview().getId()));
        assertThat(histories.selectById(f.history().getId())).isNotNull();
        assertThat(todos.selectById(f.todo().getId())).isNotNull();
    }

    @Test
    void rejectsInterviewLinkedAcrossApplications() {
        Fixture first = fixture();
        Fixture second = fixture();
        Todo invalid = new Todo();
        invalid.setApplicationId(first.application().getId());
        invalid.setInterviewId(second.interview().getId());
        invalid.setKind(TodoKind.INTERVIEW);
        invalid.setTitle("跨档案错误关联");
        assertDatabaseError(1452, () -> todos.insert(invalid));
    }

    @ParameterizedTest
    @MethodSource("invalidFacts")
    void rejectsInvalidCodesAndContradictoryFacts(String table, String assignment) {
        Fixture f = fixture();
        Long id = switch (table) {
            case "company" -> f.company().getId();
            case "application" -> f.application().getId();
            case "application_stage_history" -> f.history().getId();
            case "interview" -> f.interview().getId();
            case "todo" -> f.todo().getId();
            default -> throw new IllegalArgumentException("Unknown test table");
        };
        assertDatabaseError(3819, () -> jdbc.update(
                "UPDATE " + table + " SET " + assignment + " WHERE id = ?", id));
    }

    static Stream<Arguments> invalidFacts() {
        return Stream.of(
                Arguments.of("company", "type = 'internet'"),
                Arguments.of("application", "current_stage = 'INVALID'"),
                Arguments.of("application", "current_stage = 'submitted'"),
                Arguments.of("application", "current_stage = 'SUBMITTED '"),
                Arguments.of("application", "submitted = 2"),
                Arguments.of("application", "submitted = 0"),
                Arguments.of("application", "version = -1"),
                Arguments.of("application", "current_stage = 'ENDED'"),
                Arguments.of("application", "end_reason = 'WITHDRAWN'"),
                Arguments.of("application", "current_stage = 'ENDED', end_reason = 'INVALID'"),
                Arguments.of("application", "current_stage = 'ENDED', end_reason = 'WITHDRAWN '"),
                Arguments.of("application_stage_history", "stage = 'submitted'"),
                Arguments.of("application_stage_history", "stage = 'ENDED'"),
                Arguments.of("application_stage_history", "end_reason = 'WITHDRAWN'"),
                Arguments.of("application_stage_history", "stage = 'ENDED', end_reason = 'INVALID'"),
                Arguments.of("interview", "format = 'online'"),
                Arguments.of("todo", "kind = 'INVALID'"),
                Arguments.of("todo", "completed = 2"),
                Arguments.of("todo", "completed_at = UTC_TIMESTAMP(6)"));
    }

    @ParameterizedTest
    @EnumSource(EndReason.class)
    void storesEndedReasonInSnapshotAndHistory(EndReason reason) {
        Fixture f = fixture();
        Application application = applications.selectById(f.application().getId());
        application.setCurrentStage(ApplicationStage.ENDED);
        application.setEndReason(reason);
        assertThat(applications.updateById(application)).isEqualTo(1);
        ApplicationStageHistory history = history(application.getId(), ApplicationStage.ENDED, SUBMISSION_DATE);
        history.setEndReason(reason);
        histories.insert(history);
        assertThat(applications.selectById(application.getId()).getEndReason()).isEqualTo(reason);
        assertThat(histories.selectById(history.getId()).getEndReason()).isEqualTo(reason);
    }

    @Test
    void clearsPreviousEndingReasonAndStageDateOnAFullSnapshotUpdate() {
        Fixture f = fixture();
        Application application = applications.selectById(f.application().getId());
        application.setCurrentStage(ApplicationStage.ENDED);
        application.setEndReason(EndReason.WITHDRAWN);
        applications.updateById(application);
        application = applications.selectById(application.getId());
        application.setCurrentStage(ApplicationStage.REJECTED);
        application.setEndReason(null);
        application.setCurrentStageOn(null);
        assertThat(applications.updateById(application)).isEqualTo(1);
        Application loaded = applications.selectById(application.getId());
        assertThat(loaded.getCurrentStage()).isEqualTo(ApplicationStage.REJECTED);
        assertThat(loaded.getEndReason()).isNull();
        assertThat(loaded.getCurrentStageOn()).isNull();
    }

    @Test
    void preservesUtcInstantsAndMicrosecondsWithNonUtcJvm() {
        assertThat(TimeZone.getDefault().getID()).isEqualTo("Asia/Shanghai");
        Fixture f = fixture();
        Interview interview = interviews.selectById(f.interview().getId());
        assertThat(interview.getInterviewAt()).isEqualTo(INTERVIEW_TIME);
        assertThat(jdbc.queryForObject("SELECT @@session.time_zone", String.class)).isEqualTo("+00:00");
        LocalDateTime raw = jdbc.queryForObject(
                "SELECT interview_at FROM interview WHERE id = ?",
                (rs, row) -> rs.getObject(1, LocalDateTime.class), interview.getId());
        assertThat(raw).isEqualTo(LocalDateTime.ofInstant(INTERVIEW_TIME, ZoneOffset.UTC));

        Todo todo = todos.selectById(f.todo().getId());
        todo.setCompleted(true);
        todo.setCompletedAt(INTERVIEW_TIME.plusSeconds(3600));
        todos.updateById(todo);
        Todo loaded = todos.selectById(todo.getId());
        assertThat(loaded.getDueAt()).isEqualTo(INTERVIEW_TIME);
        assertThat(loaded.getCompletedAt()).isEqualTo(INTERVIEW_TIME.plusSeconds(3600));
        LocalDateTime created = jdbc.queryForObject("SELECT created_at FROM interview WHERE id = ?",
                (rs, row) -> rs.getObject(1, LocalDateTime.class), interview.getId());
        assertThat(created).isEqualTo(LocalDateTime.ofInstant(interview.getCreatedAt(), ZoneOffset.UTC));
        assertThat(applications.selectById(f.application().getId()).getAppliedOn()).isEqualTo(SUBMISSION_DATE);
    }

    @Test
    void rejectsStaleVersionWithoutOverwritingTheFirstUpdate() {
        Fixture f = fixture();
        Application first = applications.selectById(f.application().getId());
        sessions.clearCache();
        Application stale = applications.selectById(f.application().getId());
        assertThat(first).isNotSameAs(stale);
        first.setCurrentStage(ApplicationStage.FIRST_INTERVIEW);
        first.setCurrentStageOn(LocalDate.of(2026, 9, 24));
        assertThat(applications.updateById(first)).isEqualTo(1);
        stale.setCurrentStage(ApplicationStage.SECOND_INTERVIEW);
        assertThat(applications.updateById(stale)).isZero();
        Application loaded = applications.selectById(first.getId());
        assertThat(loaded.getVersion()).isEqualTo(1);
        assertThat(loaded.getCurrentStage()).isEqualTo(ApplicationStage.FIRST_INTERVIEW);
        assertThat(loaded.getCurrentStageOn()).isEqualTo(LocalDate.of(2026, 9, 24));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rollsBackSnapshotAndNewHistoryWhileRetainingOriginalHistory() {
        Fixture f = fixture();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
            advanceToFirstInterview(f.application().getId());
            throw new IllegalStateException("Simulated transaction failure");
        }));
        Application loaded = applications.selectById(f.application().getId());
        assertThat(loaded.getCurrentStage()).isEqualTo(ApplicationStage.SUBMITTED);
        assertThat(loaded.getVersion()).isZero();
        assertThat(historyList(loaded.getId())).extracting(ApplicationStageHistory::getId)
                .containsExactly(f.history().getId());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void commitsSnapshotAndHistoryTogetherWithoutReplacingOldHistory() {
        Fixture f = fixture();
        new TransactionTemplate(transactionManager).executeWithoutResult(
                status -> advanceToFirstInterview(f.application().getId()));
        Application loaded = applications.selectById(f.application().getId());
        assertThat(loaded.getCurrentStage()).isEqualTo(ApplicationStage.FIRST_INTERVIEW);
        assertThat(loaded.getVersion()).isEqualTo(1);
        List<ApplicationStageHistory> history = historyList(loaded.getId());
        assertThat(history).extracting(ApplicationStageHistory::getStage)
                .containsExactly(ApplicationStage.SUBMITTED, ApplicationStage.FIRST_INTERVIEW);
        assertThat(history.getFirst().getId()).isEqualTo(f.history().getId());
        assertThat(history.getFirst().getStageOn()).isEqualTo(SUBMISSION_DATE);
    }

    private void advanceToFirstInterview(Long id) {
        Application application = applications.selectById(id);
        application.setCurrentStage(ApplicationStage.FIRST_INTERVIEW);
        application.setCurrentStageOn(LocalDate.of(2026, 9, 24));
        assertThat(applications.updateById(application)).isEqualTo(1);
        histories.insert(history(id, ApplicationStage.FIRST_INTERVIEW, application.getCurrentStageOn()));
    }

    private List<ApplicationStageHistory> historyList(Long applicationId) {
        return histories.selectList(Wrappers.<ApplicationStageHistory>lambdaQuery()
                .eq(ApplicationStageHistory::getApplicationId, applicationId)
                .orderByAsc(ApplicationStageHistory::getId));
    }

    private static void assertDatabaseError(int code, Runnable write) {
        DataAccessException error = assertThrows(DataAccessException.class, write::run);
        assertThat(error.getMostSpecificCause()).isInstanceOf(SQLException.class);
        assertThat(((SQLException) error.getMostSpecificCause()).getErrorCode()).isEqualTo(code);
    }

    private JobPosition newPosition(Long companyId, String batch) {
        JobPosition position = new JobPosition();
        position.setCompanyId(companyId);
        position.setName("Java 后端开发");
        position.setLocation("上海");
        position.setDirection("Java 后端");
        position.setJd("Java / Agent 开发🌟");
        position.setRecruitmentBatch(batch);
        positions.insert(position);
        return position;
    }

    private static ApplicationStageHistory history(Long applicationId, ApplicationStage stage, LocalDate date) {
        ApplicationStageHistory history = new ApplicationStageHistory();
        history.setApplicationId(applicationId);
        history.setStage(stage);
        history.setStageOn(date);
        history.setRemark("虚构阶段记录");
        return history;
    }

    private Fixture fixture() {
        Company company = new Company();
        company.setName("星河科技🌟");
        company.setType(CompanyType.INTERNET);
        company.setWebsite("https://example.org");
        company.setNotes("虚构公司");
        company.setCreatedAt(Instant.EPOCH);
        company.setUpdatedAt(Instant.EPOCH);
        companies.insert(company);
        JobPosition position = newPosition(company.getId(), "2026 秋招");
        Application application = new Application();
        application.setJobPositionId(position.getId());
        application.setChannel("官网");
        application.setSubmitted(true);
        application.setAppliedOn(SUBMISSION_DATE);
        application.setCurrentStage(ApplicationStage.SUBMITTED);
        application.setCurrentStageOn(SUBMISSION_DATE);
        applications.insert(application);
        ApplicationStageHistory history = history(application.getId(), ApplicationStage.SUBMITTED, SUBMISSION_DATE);
        histories.insert(history);
        Interview interview = new Interview();
        interview.setApplicationId(application.getId());
        interview.setRoundName("一面");
        interview.setInterviewAt(INTERVIEW_TIME);
        interview.setFormat(InterviewFormat.ONLINE);
        interview.setQuestions("事务和并发🌟");
        interview.setAnswers("隔离级别 / 乐观锁");
        interview.setReview("复习 Spring 事务");
        interview.setResult("待反馈");
        interviews.insert(interview);
        Todo todo = new Todo();
        todo.setApplicationId(application.getId());
        todo.setInterviewId(interview.getId());
        todo.setKind(TodoKind.INTERVIEW);
        todo.setTitle("技术一面");
        todo.setDueAt(INTERVIEW_TIME);
        todos.insert(todo);
        return new Fixture(company, position, application, history, interview, todo);
    }

    record Fixture(Company company, JobPosition position, Application application,
                   ApplicationStageHistory history, Interview interview, Todo todo) {
    }
}
