package sicau.policialPartyManager.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import sicau.policialPartyManager.model.entity.User;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
