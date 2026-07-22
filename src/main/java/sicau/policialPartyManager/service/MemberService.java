package sicau.policialPartyManager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import sicau.policialPartyManager.config.JwtAuthFilter.TokenUser;
import sicau.policialPartyManager.entity.Member;
import sicau.policialPartyManager.repository.MemberMapper;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberMapper memberMapper;

    /**
     * 分页查询成员
     * super_admin 看全部，branch_admin 只能看本支部，student 只能看自己
     */
    public IPage<Member> page(int page, int size, String keyword, String college,
                               String identity, TokenUser currentUser) {
        LambdaQueryWrapper<Member> wrapper = new LambdaQueryWrapper<>();

        // 数据级权限隔离
        if ("branch_admin".equals(currentUser.role())) {
            // 通过user表的branch_id来限制，此处需要先拿到当前用户的branch_id
            // 这里简化处理：从当前登录用户关联的member获取branch
            wrapper.eq(Member::getBranchId, getCurrentUserBranchId(currentUser));
        } else if ("student".equals(currentUser.role())) {
            // 普通成员只能看自己
            wrapper.eq(Member::getStudentNo, currentUser.username());
        }

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Member::getName, keyword)
                    .or().like(Member::getStudentNo, keyword));
        }
        if (StringUtils.hasText(college)) {
            wrapper.eq(Member::getCollege, college);
        }
        if (StringUtils.hasText(identity)) {
            wrapper.eq(Member::getIdentity, identity);
        }
        wrapper.eq(Member::getStatus, 1);
        wrapper.orderByDesc(Member::getCreateTime);

        return memberMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public Member getById(Long id, TokenUser currentUser) {
        Member member = memberMapper.selectById(id);
        if (member == null) return null;

        // 权限校验
        if ("branch_admin".equals(currentUser.role())) {
            Long userBranchId = getCurrentUserBranchId(currentUser);
            if (!userBranchId.equals(member.getBranchId())) {
                throw new IllegalArgumentException("无权查看其他支部成员");
            }
        } else if ("student".equals(currentUser.role())) {
            if (!currentUser.username().equals(member.getStudentNo())) {
                throw new IllegalArgumentException("无权查看他人信息");
            }
        }
        return member;
    }

    public void save(Member member) {
        if (!StringUtils.hasText(member.getIdentity())) {
            member.setIdentity("ordinary");
        }
        memberMapper.insert(member);
    }

    public void update(Member member) {
        memberMapper.updateById(member);
    }

    public void delete(Long id, TokenUser currentUser) {
        Member member = memberMapper.selectById(id);
        if (member == null) return;
        if ("branch_admin".equals(currentUser.role())) {
            Long userBranchId = getCurrentUserBranchId(currentUser);
            if (!userBranchId.equals(member.getBranchId())) {
                throw new IllegalArgumentException("无权删除其他支部成员");
            }
        }
        member.setStatus(0);
        memberMapper.updateById(member);
    }

    /** 获取当前用户的 branch_id（从 member 表反查） */
    private Long getCurrentUserBranchId(TokenUser currentUser) {
        Member member = memberMapper.selectOne(
                new LambdaQueryWrapper<Member>().eq(Member::getStudentNo, currentUser.username()));
        return member != null ? member.getBranchId() : null;
    }
}
