package sicau.policialPartyManager.service.Impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import sicau.policialPartyManager.model.entity.Activity;
import sicau.policialPartyManager.repository.ActivityMapper;
import sicau.policialPartyManager.service.BranchScopeService;

/**
 * 活动域数据范围判定（#71 / #68 补齐）。
 * <p>超管不限；支委限本支部活动；其他角色一律拒绝。
 * <p>两类拒绝要分开，语义不同：
 * <ul>
 *   <li><b>角色不足</b>（普通成员/游客）→ {@link AccessDeniedException} → 403</li>
 *   <li><b>角色够但越支部</b> → 「活动不存在」→ 400，不泄露其他支部活动是否存在</li>
 * </ul>
 * <p>先判角色再查活动：否则普通成员对任意 id 都会得到「活动不存在」，
 * 把「你没这个权限」误报成「这东西没有」。
 */
@Service
@RequiredArgsConstructor
public class ActivityScopeGuard {

    private final ActivityMapper activityMapper;
    private final BranchScopeService branchScopeService;

    /**
     * 断言操作者可管理/查看目标活动。
     *
     * @throws AccessDeniedException    角色不足（403）
     * @throws IllegalArgumentException 活动不存在或越支部，统一「活动不存在」
     */
    public Activity requireManageable(sicau.policialPartyManager.model.records.User operator, Long activityId) {
        // 1) 角色闸门优先：非管理侧一律 403，不进入存在性判断
        if (!branchScopeService.isSuperAdmin(operator) && !branchScopeService.isBranchAdmin(operator)) {
            throw new AccessDeniedException("无权访问该活动的管理功能");
        }

        // 2) 存在性 + 支部归属
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new IllegalArgumentException("活动不存在");
        }
        if (branchScopeService.isSuperAdmin(operator)) {
            return activity;
        }
        Long mine = branchScopeService.branchIdOf(operator);
        if (mine == null || activity.getBranchId() == null || !mine.equals(activity.getBranchId())) {
            // 越权与真不存在同文案
            throw new IllegalArgumentException("活动不存在");
        }
        return activity;
    }
}
