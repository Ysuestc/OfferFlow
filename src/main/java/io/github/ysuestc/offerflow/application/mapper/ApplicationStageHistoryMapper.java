package io.github.ysuestc.offerflow.application.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.ysuestc.offerflow.application.entity.ApplicationStageHistory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ApplicationStageHistoryMapper extends BaseMapper<ApplicationStageHistory> {
}
