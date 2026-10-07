package io.github.ysuestc.offerflow.todo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.ysuestc.offerflow.todo.entity.Todo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TodoMapper extends BaseMapper<Todo> {
}
