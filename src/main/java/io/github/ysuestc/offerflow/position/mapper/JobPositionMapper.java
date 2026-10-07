package io.github.ysuestc.offerflow.position.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.ysuestc.offerflow.position.entity.JobPosition;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface JobPositionMapper extends BaseMapper<JobPosition> {
}
