package io.github.ysuestc.offerflow.todo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.ysuestc.offerflow.todo.entity.*;
import io.github.ysuestc.offerflow.todo.vo.TodoView;
import java.time.Instant;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface TodoMapper extends BaseMapper<Todo> {
    @Select("""
        <script>
        SELECT x.id, x.application_id, c.name AS company_name, p.name AS position_name, x.interview_id, i.round_name AS interview_round, x.kind, x.title, x.due_at, x.completed, x.completed_at, x.notes, x.version, x.updated_at FROM todo x JOIN application a ON a.id = x.application_id JOIN job_position p ON p.id = a.job_position_id JOIN company c ON c.id = p.company_id LEFT JOIN interview i ON i.id = x.interview_id
        <where>
            <if test="q != null"> (c.name LIKE CONCAT('%', #{q}, '%') ESCAPE '!'
                OR p.name LIKE CONCAT('%', #{q}, '%') ESCAPE '!'
                OR x.title LIKE CONCAT('%', #{q}, '%') ESCAPE '!') </if>
            <if test="applicationId != null"> AND x.application_id = #{applicationId} </if>
            <if test="kind != null"> AND x.kind = #{kind} </if>
            <if test="completed != null"> AND x.completed = #{completed} </if>
            <if test="timing != null"> AND x.completed = 0
                <choose>
                    <when test="timing.name() == 'UPCOMING'"> AND x.due_at &gt;= #{now} AND x.due_at &lt; #{until} </when>
                    <when test="timing.name() == 'OVERDUE'"> AND x.due_at &lt; #{now} </when>
                    <otherwise> AND x.due_at IS NULL </otherwise>
                </choose>
            </if>
        </where>
        ORDER BY x.completed, x.due_at IS NULL, x.due_at, x.id LIMIT #{size} OFFSET #{offset}
        </script>
        """)
    List<TodoView> listViews(@Param("q") String q, @Param("applicationId") Long applicationId,
            @Param("kind") TodoKind kind, @Param("completed") Boolean completed,
            @Param("timing") TodoTiming timing, @Param("now") Instant now, @Param("until") Instant until,
            @Param("size") int size, @Param("offset") long offset);

    @Select("""
        <script>
        SELECT COUNT(*) FROM todo x JOIN application a ON a.id = x.application_id JOIN job_position p ON p.id = a.job_position_id JOIN company c ON c.id = p.company_id LEFT JOIN interview i ON i.id = x.interview_id
        <where>
            <if test="q != null"> (c.name LIKE CONCAT('%', #{q}, '%') ESCAPE '!'
                OR p.name LIKE CONCAT('%', #{q}, '%') ESCAPE '!'
                OR x.title LIKE CONCAT('%', #{q}, '%') ESCAPE '!') </if>
            <if test="applicationId != null"> AND x.application_id = #{applicationId} </if>
            <if test="kind != null"> AND x.kind = #{kind} </if>
            <if test="completed != null"> AND x.completed = #{completed} </if>
            <if test="timing != null"> AND x.completed = 0
                <choose>
                    <when test="timing.name() == 'UPCOMING'"> AND x.due_at &gt;= #{now} AND x.due_at &lt; #{until} </when>
                    <when test="timing.name() == 'OVERDUE'"> AND x.due_at &lt; #{now} </when>
                    <otherwise> AND x.due_at IS NULL </otherwise>
                </choose>
            </if>
        </where>
        </script>
        """)
    long countViews(@Param("q") String q, @Param("applicationId") Long applicationId,
            @Param("kind") TodoKind kind, @Param("completed") Boolean completed,
            @Param("timing") TodoTiming timing, @Param("now") Instant now, @Param("until") Instant until);

    @Select("""
        SELECT x.id, x.application_id, c.name AS company_name, p.name AS position_name, x.interview_id, i.round_name AS interview_round, x.kind, x.title, x.due_at, x.completed, x.completed_at, x.notes, x.version, x.updated_at FROM todo x JOIN application a ON a.id = x.application_id JOIN job_position p ON p.id = a.job_position_id JOIN company c ON c.id = p.company_id LEFT JOIN interview i ON i.id = x.interview_id WHERE x.id = #{id}
        """)
    TodoView findView(long id);
}
