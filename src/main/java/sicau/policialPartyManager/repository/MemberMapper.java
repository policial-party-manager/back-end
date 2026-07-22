package sicau.policialPartyManager.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import sicau.policialPartyManager.entity.Member;

@Mapper
public interface MemberMapper extends BaseMapper<Member> {
}
