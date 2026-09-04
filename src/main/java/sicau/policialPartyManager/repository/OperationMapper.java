package sicau.policialPartyManager.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import sicau.policialPartyManager.model.entity.Operation;

@Mapper
public interface OperationMapper extends BaseMapper<Operation> {
}
