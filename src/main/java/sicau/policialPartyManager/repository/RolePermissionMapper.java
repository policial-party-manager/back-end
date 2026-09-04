package sicau.policialPartyManager.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import sicau.policialPartyManager.model.entity.RolePermission;

@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {
}
