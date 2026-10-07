package io.github.ysuestc.offerflow.mailbox.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.ysuestc.offerflow.mailbox.entity.MailMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MailMessageMapper extends BaseMapper<MailMessage> {}
