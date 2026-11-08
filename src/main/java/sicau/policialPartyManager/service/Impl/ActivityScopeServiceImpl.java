package sicau.policialPartyManager.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import sicau.policialPartyManager.model.entity.ActivityIdentity;
import sicau.policialPartyManager.model.entity.UserDetail;
import sicau.policialPartyManager.repository.ActivityIdentityMapper;
import sicau.policialPartyManager.repository.UserDetailMapper;
import sicau.policialPartyManager.service.ActivityScopeService;

import java.util.List;

/**
 * 活动参与范围判定实现（#69 提取的单一口径）。
 * <p>规则逐条对应报名侧原本的私有实现，行为等价：
 * <ul>
 *   <li>无范围行 → 全场开放</li>
 *   <li>命中任意一行 → 通过</li>
 *   <li>行上 college 为空 = 不限学院；grade 为空 = 不限年级；targetIdentity 为空 = 不限身份</li>
 *   <li>受限身份活动且用户未登记政治身份 → 可执行提示</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ActivityScopeServiceImpl implements ActivityScopeService {

    private final ActivityIdentityMapper activityIdentityMapper;
    private final UserDetailMapper userDetailMapper;

    @Override
    public List<ActivityIdentity> rowsOf(Long activityId) {
        return activityIdentityMapper.selectList(
                new LambdaQueryWrapper<ActivityIdentity>()
                        .eq(ActivityIdentity::getActivityId, activityId));
    }

    @Override
    public boolean isEligible(Long activityId, Long userId) {
        try {
            requireEligible(activityId, userId);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    @Override
    public void requireEligible(Long activityId, Long userId) {
        List<ActivityIdentity> rows = rowsOf(activityId);
        if (rows == null || rows.isEmpty()) {
            return;
        }
        UserDetail user = userDetailMapper.selectById(userId);
        if (user != null) {
            String userGrade = trimOrEmpty(user.getGrade());
            String userCollege = trimOrEmpty(user.getCollege());
            boolean anyRowNeedsIdentity = rows.stream()
                    .anyMatch(row -> row.getTargetIdentity() != null);
            for (ActivityIdentity row : rows) {
                boolean collegeOk = !StringUtils.hasText(row.getCollege())
                        || row.getCollege().trim().equals(userCollege);
                boolean gradeOk = row.getGrade() == null
                        || String.valueOf(row.getGrade()).equals(userGrade);
                boolean identityOk = row.getTargetIdentity() == null
                        || row.getTargetIdentity().equals(user.getIdentityId());
                if (collegeOk && gradeOk && identityOk) {
                    return;
                }
            }
            if (anyRowNeedsIdentity && user.getIdentityId() == null) {
                throw new IllegalArgumentException(
                        "您的政治身份未登记，无法报名限定身份的活动，请联系支部管理员提交身份变更");
            }
        }
        throw new IllegalArgumentException("您不符合该活动的报名范围（年级/学院/政治身份）");
    }

    @Override
    public Integer expectedCount(Long activityId) {
        List<ActivityIdentity> rows = rowsOf(activityId);
        if (rows == null || rows.isEmpty()) {
            // 无范围行 = 全场开放，应到不可界定
            return null;
        }
        return expectedUserIds(activityId).size();
    }

    @Override
    public java.util.List<Long> expectedUserIds(Long activityId) {
        List<ActivityIdentity> rows = rowsOf(activityId);
        java.util.List<Long> ids = new java.util.ArrayList<>();
        if (rows == null || rows.isEmpty()) {
            return ids;
        }
        List<UserDetail> all = userDetailMapper.selectList(null);
        for (UserDetail user : all) {
            if (matchesAnyRow(rows, user)) {
                ids.add(user.getUserId());
            }
        }
        return ids;
    }

    /** 与 requireEligible 完全相同的匹配逻辑，但作用在「无差别遍历成员」场景。 */
    private boolean matchesAnyRow(List<ActivityIdentity> rows, UserDetail user) {
        String userGrade = trimOrEmpty(user.getGrade());
        String userCollege = trimOrEmpty(user.getCollege());
        for (ActivityIdentity row : rows) {
            boolean collegeOk = !StringUtils.hasText(row.getCollege())
                    || row.getCollege().trim().equals(userCollege);
            boolean gradeOk = row.getGrade() == null
                    || String.valueOf(row.getGrade()).equals(userGrade);
            boolean identityOk = row.getTargetIdentity() == null
                    || row.getTargetIdentity().equals(user.getIdentityId());
            if (collegeOk && gradeOk && identityOk) {
                return true;
            }
        }
        return false;
    }

    private String trimOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
