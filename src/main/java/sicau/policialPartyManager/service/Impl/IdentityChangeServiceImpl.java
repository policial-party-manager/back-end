package sicau.policialPartyManager.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sicau.policialPartyManager.api.dto.IdentityChangeApplyRequest;
import sicau.policialPartyManager.api.dto.IdentityChangeVo;
import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.model.entity.Branch;
import sicau.policialPartyManager.model.entity.Identity;
import sicau.policialPartyManager.model.entity.IdentityChange;
import sicau.policialPartyManager.model.entity.UserDetail;
import sicau.policialPartyManager.model.records.User;
import sicau.policialPartyManager.repository.BranchMapper;
import sicau.policialPartyManager.repository.IdentityChangeMapper;
import sicau.policialPartyManager.repository.IdentityMapper;
import sicau.policialPartyManager.repository.UserDetailMapper;
import sicau.policialPartyManager.service.IdentityChangeService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 身份变更单（#44，两阶段）。
 * <p>
 * 依赖集合里没有 UserRoleMapper——「身份变更永不联动登录角色」在构造层面即不可能被违反。
 * 越权读取/审批他人单据统一「变更单不存在」语义；支管越支部统一「用户不存在」语义。
 */
@Service
@RequiredArgsConstructor
public class IdentityChangeServiceImpl implements IdentityChangeService {

    private static final String ROLE_SUPER_ADMIN = "super_admin";
    private static final String ORDER_NOT_FOUND = "变更单不存在";

    private final IdentityChangeMapper changeMapper;
    private final IdentityMapper identityMapper;
    private final UserDetailMapper userDetailMapper;
    private final BranchMapper branchMapper;

    @Override
    @Transactional
    public IdentityChangeVo apply(IdentityChangeApplyRequest request, User operator) {
        Set<Long> managed = null;
        if (!isSuperAdmin(operator)) {
            managed = managedBranchIds(operator.userId());
            if (managed.isEmpty()) {
                throw new IllegalArgumentException("您未管理任何支部（不是任何支部的秘书），无法发起身份变更");
            }
        }
        UserDetail target = userDetailMapper.selectById(request.getUserId());
        if (target == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (managed != null && (target.getBranchId() == null || !managed.contains(target.getBranchId()))) {
            // 他支部成员与不存在同语义
            throw new IllegalArgumentException("用户不存在");
        }
        Identity to = identityMapper.selectById(request.getToIdentity());
        if (to == null) {
            throw new IllegalArgumentException("目标身份不存在");
        }
        if (Objects.equals(target.getIdentityId(), to.getId())) {
            throw new IllegalArgumentException("目标身份与当前身份相同，无需变更");
        }
        Long pending = changeMapper.selectCount(new LambdaQueryWrapper<IdentityChange>()
                .eq(IdentityChange::getUserId, target.getUserId())
                .eq(IdentityChange::getStatus, IdentityChange.STATUS_PENDING));
        if (pending != null && pending > 0) {
            throw new IllegalArgumentException("该用户已有待审批的身份变更单");
        }

        IdentityChange order = new IdentityChange();
        order.setUserId(target.getUserId());
        order.setFromIdentity(target.getIdentityId()); // 发起时快照；首次登记为 null
        order.setToIdentity(to.getId());
        order.setOperatorId(operator.userId());
        order.setReason(trimToNull(request.getReason()));
        order.setStatus(IdentityChange.STATUS_PENDING);
        order.setCreateTime(LocalDateTime.now());
        changeMapper.insert(order);
        return toVo(order, target.getName(), identityName(order.getFromIdentity()),
                to.getIdentityName(), operatorName(order.getOperatorId()), null);
    }

    @Override
    @Transactional
    public IdentityChangeVo approve(Long id, Boolean pass, String remark, User reviewer) {
        if (!isSuperAdmin(reviewer)) {
            throw new IllegalArgumentException("仅超级管理员可审批身份变更");
        }
        IdentityChange order = changeMapper.selectById(id);
        if (order == null) {
            throw new IllegalArgumentException(ORDER_NOT_FOUND);
        }
        if (!Integer.valueOf(IdentityChange.STATUS_PENDING).equals(order.getStatus())) {
            throw new IllegalArgumentException("该变更单已处理，不能重复审批");
        }
        boolean approved = Boolean.TRUE.equals(pass);
        order.setStatus(approved ? IdentityChange.STATUS_APPROVED : IdentityChange.STATUS_REJECTED);
        order.setReviewerId(reviewer.userId());
        order.setReviewTime(LocalDateTime.now());
        order.setReviewRemark(trimToNull(remark));

        UserDetail target = userDetailMapper.selectById(order.getUserId());
        if (approved) {
            if (target == null) {
                throw new IllegalArgumentException("目标用户档案不存在，无法生效");
            }
            // 生效写回当前身份；登录角色不在此处（也不在任何处）被触碰
            target.setIdentityId(order.getToIdentity());
            userDetailMapper.updateById(target);
        }
        changeMapper.updateById(order);
        return toVo(order, target == null ? null : target.getName(),
                identityName(order.getFromIdentity()), identityName(order.getToIdentity()),
                operatorName(order.getOperatorId()), reviewer.username());
    }

    @Override
    public IdentityChangeVo detail(Long id, User viewer) {
        IdentityChange order = changeMapper.selectById(id);
        if (order == null) {
            throw new IllegalArgumentException(ORDER_NOT_FOUND);
        }
        boolean visible;
        if (isSuperAdmin(viewer)) {
            visible = true;
        } else if (Objects.equals(order.getUserId(), viewer.userId())) {
            visible = true;
        } else {
            Set<Long> managed = managedBranchIds(viewer.userId());
            UserDetail target = userDetailMapper.selectById(order.getUserId());
            visible = target != null && target.getBranchId() != null && managed.contains(target.getBranchId());
        }
        if (!visible) {
            // 越权与不存在同语义，不泄露单据存在性
            throw new IllegalArgumentException(ORDER_NOT_FOUND);
        }
        UserDetail target = userDetailMapper.selectById(order.getUserId());
        return toVo(order, target == null ? null : target.getName(),
                identityName(order.getFromIdentity()), identityName(order.getToIdentity()),
                operatorName(order.getOperatorId()),
                order.getReviewerId() == null ? null : operatorName(order.getReviewerId()));
    }

    @Override
    public PageResult<IdentityChangeVo> pageMine(long page, long size, User user) {
        return page(page, size, new LambdaQueryWrapper<IdentityChange>()
                .eq(IdentityChange::getUserId, user.userId())
                .orderByDesc(IdentityChange::getCreateTime));
    }

    @Override
    public PageResult<IdentityChangeVo> pageForBranch(long page, long size, User operator) {
        if (isSuperAdmin(operator)) {
            return pageForAdmin(page, size, null, null);
        }
        Set<Long> managed = managedBranchIds(operator.userId());
        if (managed.isEmpty()) {
            throw new IllegalArgumentException("您未管理任何支部（不是任何支部的秘书）");
        }
        List<Long> memberIds = userDetailMapper.selectList(new LambdaQueryWrapper<UserDetail>()
                        .in(UserDetail::getBranchId, managed)).stream()
                .map(UserDetail::getUserId).toList();
        if (memberIds.isEmpty()) {
            return PageResult.of(List.of(), 0L, page, size);
        }
        return page(page, size, new LambdaQueryWrapper<IdentityChange>()
                .in(IdentityChange::getUserId, memberIds)
                .orderByDesc(IdentityChange::getCreateTime));
    }

    @Override
    public PageResult<IdentityChangeVo> pageForAdmin(long page, long size, Long userId, Integer status) {
        LambdaQueryWrapper<IdentityChange> wrapper = new LambdaQueryWrapper<IdentityChange>()
                .orderByDesc(IdentityChange::getCreateTime);
        if (userId != null) {
            wrapper.eq(IdentityChange::getUserId, userId);
        }
        if (status != null) {
            wrapper.eq(IdentityChange::getStatus, status);
        }
        return page(page, size, wrapper);
    }

    // ---------- 私有 ----------

    private boolean isSuperAdmin(User operator) {
        return ROLE_SUPER_ADMIN.equalsIgnoreCase(operator.role());
    }

    private Set<Long> managedBranchIds(Long operatorUserId) {
        return branchMapper.selectList(new LambdaQueryWrapper<Branch>()
                        .eq(Branch::getSecretaryId, operatorUserId)
                        .eq(Branch::getStatus, 1)).stream()
                .map(Branch::getId).collect(Collectors.toSet());
    }

    private PageResult<IdentityChangeVo> page(long page, long size, LambdaQueryWrapper<IdentityChange> wrapper) {
        Page<IdentityChange> result = changeMapper.selectPage(new Page<>(page, size), wrapper);
        List<IdentityChange> rows = result.getRecords();
        Set<Long> userIds = collect(rows, IdentityChange::getUserId, IdentityChange::getOperatorId,
                IdentityChange::getReviewerId);
        Set<Long> identityIds = collect(rows, IdentityChange::getFromIdentity, IdentityChange::getToIdentity);
        Map<Long, String> userNames = userIds.isEmpty() ? Map.of()
                : userDetailMapper.selectBatchIds(userIds).stream()
                .filter(d -> d.getName() != null)
                .collect(Collectors.toMap(UserDetail::getUserId, UserDetail::getName, (a, b) -> a));
        Map<Long, String> identityNames = identityIds.isEmpty() ? Map.of()
                : identityMapper.selectBatchIds(identityIds).stream()
                .filter(i -> i.getIdentityName() != null)
                .collect(Collectors.toMap(Identity::getId, Identity::getIdentityName, (a, b) -> a));
        List<IdentityChangeVo> vos = rows.stream().map(o -> toVo(o,
                userNames.get(o.getUserId()),
                identityNames.get(o.getFromIdentity()),
                identityNames.get(o.getToIdentity()),
                userNames.get(o.getOperatorId()),
                userNames.get(o.getReviewerId()))).toList();
        return PageResult.of(vos, result.getTotal(), page, size);
    }

    @SafeVarargs
    private final Set<Long> collect(List<IdentityChange> rows,
                                    java.util.function.Function<IdentityChange, Long>... getters) {
        return rows.stream()
                .flatMap(row -> Stream.of(getters).map(g -> g.apply(row)))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private IdentityChangeVo toVo(IdentityChange order, String userName, String fromName,
                                  String toName, String operatorName, String reviewerName) {
        IdentityChangeVo vo = new IdentityChangeVo();
        vo.setId(order.getId());
        vo.setUserId(order.getUserId());
        vo.setUserName(userName);
        vo.setFromIdentity(order.getFromIdentity());
        vo.setFromIdentityName(fromName);
        vo.setToIdentity(order.getToIdentity());
        vo.setToIdentityName(toName);
        vo.setOperatorId(order.getOperatorId());
        vo.setOperatorName(operatorName);
        vo.setReason(order.getReason());
        vo.setStatus(order.getStatus());
        vo.setStatusName(statusName(order.getStatus()));
        vo.setReviewerId(order.getReviewerId());
        vo.setReviewerName(reviewerName);
        vo.setReviewTime(order.getReviewTime());
        vo.setReviewRemark(order.getReviewRemark());
        vo.setCreateTime(order.getCreateTime());
        return vo;
    }

    private String statusName(Integer status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case IdentityChange.STATUS_PENDING -> "待审批";
            case IdentityChange.STATUS_APPROVED -> "已生效";
            case IdentityChange.STATUS_REJECTED -> "驳回";
            default -> "未知";
        };
    }

    private String identityName(Long identityId) {
        if (identityId == null) {
            return null;
        }
        Identity identity = identityMapper.selectById(identityId);
        return identity == null ? null : identity.getIdentityName();
    }

    private String operatorName(Long userId) {
        UserDetail detail = userDetailMapper.selectById(userId);
        return detail == null ? null : detail.getName();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
