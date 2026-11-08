package sicau.policialPartyManager.service.Impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sicau.policialPartyManager.model.entity.Activity;
import sicau.policialPartyManager.repository.ActivityMapper;
import sicau.policialPartyManager.service.BranchScopeService;

/**
 * 活动域数据范围判定（#71）。
 * <p>超管不限；支委限本支部活动；其余拒绝。
 * <p>越权与真不存在同文案（不存在语义）。
 */
@Service
@RequiredArgsConstructor
public class ActivityScopeGuard {

    private final ActivityMapper activityMapper;
    private final BranchScopeService branchScopeService;

    /**
     * 断言操作者可管理/查看目标活动。
     *
     * @throws IllegalArgumentException 活动不存在或不在操作者范围内，统一「活动不存在」
     */
    public Activity requireManageable(sicau.policialPartyManager.model.records.User operator, Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new IllegalArgumentException("活动不存在");
        }
        if (branchScopeService.isSuperAdmin(operator)) {
            return activity;
        }
        if (!branchScopeService.isBranchAdmin(operator)) {
            throw new IllegalArgumentException("活动不存在");
        }
        Long mine = branchScopeService.branchIdOf(operator);
        if (mine == null || activity.getBranchId() == null || !mine.equals(activity.getBranchId())) {
            throw new IllegalArgumentException("活动不存在");
        }
        return activity;
    }
}
