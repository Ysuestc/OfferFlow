package io.github.ysuestc.offerflow.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.ysuestc.offerflow.interview.entity.Interview;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InterviewMapper extends BaseMapper<Interview> {
}
