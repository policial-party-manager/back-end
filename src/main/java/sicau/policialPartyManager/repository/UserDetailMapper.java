package sicau.policialPartyManager.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import sicau.policialPartyManager.model.entity.UserDetail;

@Mapper
public interface UserDetailMapper extends BaseMapper<UserDetail> {
}
