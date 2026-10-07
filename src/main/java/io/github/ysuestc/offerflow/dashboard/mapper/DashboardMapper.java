package io.github.ysuestc.offerflow.dashboard.mapper;

import io.github.ysuestc.offerflow.application.vo.ApplicationView;
import io.github.ysuestc.offerflow.dashboard.vo.DashboardCounts;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DashboardMapper {
    @Select("""
        SELECT COUNT(CASE WHEN submitted = 1 THEN 1 END) AS total_submitted,
            COUNT(CASE WHEN submitted = 1 AND current_stage NOT IN
                ('TO_APPLY', 'OFFER', 'REJECTED', 'ENDED') THEN 1 END) AS active,
            COUNT(CASE WHEN submitted = 1 AND current_stage IN
                ('FIRST_INTERVIEW', 'SECOND_INTERVIEW', 'THIRD_INTERVIEW', 'HR_INTERVIEW')
                THEN 1 END) AS interviewing,
            COUNT(CASE WHEN current_stage = 'OFFER' THEN 1 END) AS offers,
            COUNT(CASE WHEN current_stage = 'REJECTED' THEN 1 END) AS rejected
        FROM application
        """)
    DashboardCounts counts();

    @Select("""
        SELECT a.id, a.job_position_id, p.company_id, c.name AS company_name,
            p.name AS position_name, p.location, p.direction, p.recruitment_batch,
            a.channel, a.applied_on, a.submitted, a.current_stage, a.current_stage_on,
            a.end_reason, a.notes, a.version, a.updated_at
        FROM application a JOIN job_position p ON p.id = a.job_position_id
            JOIN company c ON c.id = p.company_id
        WHERE a.submitted = 1 ORDER BY a.applied_on DESC, a.id DESC LIMIT 5
        """)
    List<ApplicationView> recentApplications();
}
