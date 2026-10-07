package io.github.ysuestc.offerflow.position.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.ysuestc.offerflow.position.entity.JobPosition;
import io.github.ysuestc.offerflow.position.vo.PositionView;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface JobPositionMapper extends BaseMapper<JobPosition> {
    @Select("""
        <script>
        SELECT p.id, p.company_id, c.name AS company_name, p.name, p.location, p.direction, p.jd, p.recruitment_batch FROM job_position p JOIN company c ON c.id = p.company_id
        <where>
            <if test="q != null"> (p.name LIKE CONCAT('%', #{q}, '%') ESCAPE '!' OR c.name LIKE CONCAT('%', #{q}, '%') ESCAPE '!') </if>
            <if test="companyId != null"> AND p.company_id = #{companyId} </if>
        </where>
        ORDER BY p.id DESC LIMIT #{size} OFFSET #{offset}
        </script>
        """)
    List<PositionView> listViews(@Param("q") String q, @Param("companyId") Long companyId,
            @Param("size") int size, @Param("offset") long offset);

    @Select("""
        <script>
        SELECT COUNT(*) FROM job_position p JOIN company c ON c.id = p.company_id
        <where>
            <if test="q != null"> (p.name LIKE CONCAT('%', #{q}, '%') ESCAPE '!' OR c.name LIKE CONCAT('%', #{q}, '%') ESCAPE '!') </if>
            <if test="companyId != null"> AND p.company_id = #{companyId} </if>
        </where>
        </script>
        """)
    long countViews(@Param("q") String q, @Param("companyId") Long companyId);

    @Select("SELECT p.id, p.company_id, c.name AS company_name, p.name, p.location, p.direction, p.jd, p.recruitment_batch FROM job_position p JOIN company c ON c.id = p.company_id WHERE p.id = #{id}")
    PositionView findView(long id);
}
