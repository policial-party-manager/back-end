package sicau.policialPartyManager.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import sicau.policialPartyManager.entity.Permission;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {
}
