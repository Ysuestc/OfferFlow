package io.github.ysuestc.offerflow.application.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.ysuestc.offerflow.application.entity.Application;
import io.github.ysuestc.offerflow.application.entity.ApplicationStage;
import io.github.ysuestc.offerflow.application.vo.ApplicationView;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ApplicationMapper extends BaseMapper<Application> {
    @Select("""
        <script>
        SELECT a.id, a.job_position_id, p.company_id, c.name AS company_name, p.name AS position_name, p.location, p.direction, p.recruitment_batch, a.channel, a.applied_on, a.submitted, a.current_stage, a.current_stage_on, a.end_reason, a.notes, a.version, a.updated_at FROM application a JOIN job_position p ON p.id = a.job_position_id JOIN company c ON c.id = p.company_id
        <where>
            <if test="q != null"> (p.name LIKE CONCAT('%', #{q}, '%') ESCAPE '!' OR c.name LIKE CONCAT('%', #{q}, '%') ESCAPE '!') </if>
            <if test="stage != null"> AND a.current_stage = #{stage} </if>
        </where>
        ORDER BY a.id DESC LIMIT #{size} OFFSET #{offset}
        </script>
        """)
    List<ApplicationView> listViews(@Param("q") String q, @Param("stage") ApplicationStage stage,
            @Param("size") int size, @Param("offset") long offset);

    @Select("""
        <script>
        SELECT COUNT(*) FROM application a JOIN job_position p ON p.id = a.job_position_id JOIN company c ON c.id = p.company_id
        <where>
            <if test="q != null"> (p.name LIKE CONCAT('%', #{q}, '%') ESCAPE '!' OR c.name LIKE CONCAT('%', #{q}, '%') ESCAPE '!') </if>
            <if test="stage != null"> AND a.current_stage = #{stage} </if>
        </where>
        </script>
        """)
    long countViews(@Param("q") String q, @Param("stage") ApplicationStage stage);

    @Select("SELECT a.id, a.job_position_id, p.company_id, c.name AS company_name, p.name AS position_name, p.location, p.direction, p.recruitment_batch, a.channel, a.applied_on, a.submitted, a.current_stage, a.current_stage_on, a.end_reason, a.notes, a.version, a.updated_at FROM application a JOIN job_position p ON p.id = a.job_position_id JOIN company c ON c.id = p.company_id WHERE a.id = #{id}")
    ApplicationView findView(long id);
}
