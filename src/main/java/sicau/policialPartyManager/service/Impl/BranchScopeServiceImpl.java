package sicau.policialPartyManager.service.Impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sicau.policialPartyManager.model.entity.UserDetail;
import sicau.policialPartyManager.model.enums.UserRole;
import sicau.policialPartyManager.model.records.User;
import sicau.policialPartyManager.repository.UserDetailMapper;
import sicau.policialPartyManager.service.BranchScopeService;

/**
 * 支委数据范围判定实现（#70）。
 * <p>判据是成员所属支部 {@code tb_user_detail.branch_id}，不是 {@code tb_branch.secretary_id}。
 * 书记身份只用于派定思想汇报审阅人，不参与鉴权。
 */
@Service
@RequiredArgsConstructor
public class BranchScopeServiceImpl implements BranchScopeService {

    private final UserDetailMapper userDetailMapper;

    @Override
    public boolean isSuperAdmin(User operator) {
        return operator != null
                && UserRole.SUPER_ADMIN.getCode().equalsIgnoreCase(operator.role());
    }

    @Override
    public boolean isBranchAdmin(User operator) {
        return operator != null
                && UserRole.BRANCH_ADMIN.getCode().equalsIgnoreCase(operator.role());
    }

    @Override
    public Long branchIdOf(User operator) {
        if (!isBranchAdmin(operator) || operator.userId() == null) {
            return null;
        }
        UserDetail detail = userDetailMapper.selectById(operator.userId());
        return detail == null ? null : detail.getBranchId();
    }

    @Override
    public void requireManageableBranch(User operator, Long targetBranchId) {
        if (isSuperAdmin(operator)) {
            return;
        }
        if (!isBranchAdmin(operator)) {
            throw new IllegalArgumentException("无权访问该数据");
        }
        Long mine = branchIdOf(operator);
        if (mine == null || !mine.equals(targetBranchId)) {
            // 不存在语义：不泄露其他支部的数据是否存在
            throw new IllegalArgumentException("支部不存在");
        }
    }

    @Override
    public void requireManageableUser(User operator, Long targetUserId) {
        if (isSuperAdmin(operator)) {
            return;
        }
        if (!isBranchAdmin(operator)) {
            throw new IllegalArgumentException("用户不存在");
        }
        Long mine = branchIdOf(operator);
        if (mine == null || targetUserId == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        UserDetail target = userDetailMapper.selectById(targetUserId);
        if (target == null || !mine.equals(target.getBranchId())) {
            // 越权与真不存在同文案
            throw new IllegalArgumentException("用户不存在");
        }
    }
}
