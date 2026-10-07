package io.github.ysuestc.offerflow.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.ysuestc.offerflow.interview.entity.*;
import io.github.ysuestc.offerflow.interview.vo.*;
import java.time.Instant;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface InterviewMapper extends BaseMapper<Interview> {
    @Select("""
        <script>
        SELECT x.id, x.application_id, c.name AS company_name, p.name AS position_name, x.round_name, x.interview_at, x.format, x.version, x.updated_at FROM interview x JOIN application a ON a.id = x.application_id JOIN job_position p ON p.id = a.job_position_id JOIN company c ON c.id = p.company_id
        <where>
            <if test="q != null"> (c.name LIKE CONCAT('%', #{q}, '%') ESCAPE '!'
                OR p.name LIKE CONCAT('%', #{q}, '%') ESCAPE '!'
                OR x.round_name LIKE CONCAT('%', #{q}, '%') ESCAPE '!') </if>
            <if test="applicationId != null"> AND x.application_id = #{applicationId} </if>
            <if test="format != null"> AND x.format = #{format} </if>
        </where>
        ORDER BY x.interview_at IS NULL, x.interview_at DESC, x.id DESC LIMIT #{size} OFFSET #{offset}
        </script>
        """)
    List<InterviewSummary> listViews(@Param("q") String q, @Param("applicationId") Long applicationId,
            @Param("format") InterviewFormat format, @Param("size") int size, @Param("offset") long offset);

    @Select("""
        <script>
        SELECT COUNT(*) FROM interview x JOIN application a ON a.id = x.application_id JOIN job_position p ON p.id = a.job_position_id JOIN company c ON c.id = p.company_id
        <where>
            <if test="q != null"> (c.name LIKE CONCAT('%', #{q}, '%') ESCAPE '!'
                OR p.name LIKE CONCAT('%', #{q}, '%') ESCAPE '!'
                OR x.round_name LIKE CONCAT('%', #{q}, '%') ESCAPE '!') </if>
            <if test="applicationId != null"> AND x.application_id = #{applicationId} </if>
            <if test="format != null"> AND x.format = #{format} </if>
        </where>
        </script>
        """)
    long countViews(@Param("q") String q, @Param("applicationId") Long applicationId,
            @Param("format") InterviewFormat format);

    @Select("""
        SELECT x.id, x.application_id, c.name AS company_name, p.name AS position_name,
            x.round_name, x.interview_at, x.format, x.questions, x.answers, x.review, x.result,
            x.version, x.updated_at
        FROM interview x JOIN application a ON a.id = x.application_id JOIN job_position p ON p.id = a.job_position_id JOIN company c ON c.id = p.company_id WHERE x.id = #{id}
        """)
    InterviewView findView(long id);

    @Select("""
        SELECT x.id, x.application_id, c.name AS company_name, p.name AS position_name, x.round_name, x.interview_at, x.format, x.version, x.updated_at FROM interview x JOIN application a ON a.id = x.application_id JOIN job_position p ON p.id = a.job_position_id JOIN company c ON c.id = p.company_id
        WHERE x.interview_at <= #{now}
        ORDER BY x.interview_at DESC, x.id DESC LIMIT 5
        """)
    List<InterviewSummary> recent(Instant now);
}
