package sicau.policialPartyManager.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import sicau.policialPartyManager.model.entity.ActivityLeave;
import sicau.policialPartyManager.repository.ActivityLeaveMapper;

import java.util.List;

/**
 * 活动请假（#78）。
 * <p>请假是「已批准的预期」，与签到记录（已发生的事实）生命周期不同，因此独立成表。
 * <p>同一成员在同一活动下只有一个请假行（唯一约束），撤销为软删除（status=0）。
 */
@Service
@RequiredArgsConstructor
public class ActivityLeaveServiceImpl {

    private final ActivityLeaveMapper leaveMapper;

    /** 录入请假；已存在生效记录则更新理由并返回。 */
    @Transactional
    public Long enroll(Long activityId, Long userId, String reason, Long operatorId) {
        ActivityLeave existed = leaveMapper.selectOne(new LambdaQueryWrapper<ActivityLeave>()
                .eq(ActivityLeave::getActivityId, activityId)
                .eq(ActivityLeave::getUserId, userId));
        if (existed != null) {
            existed.setReason(trimToNull(reason));
            existed.setOperatorId(operatorId);
            existed.setStatus(1);
            leaveMapper.updateById(existed);
            return existed.getId();
        }
        ActivityLeave leave = new ActivityLeave();
        leave.setActivityId(activityId);
        leave.setUserId(userId);
        leave.setReason(trimToNull(reason));
        leave.setOperatorId(operatorId);
        leave.setStatus(1);
        leaveMapper.insert(leave);
        return leave.getId();
    }

    /** 撤销请假（status=0）；不存在或已撤销时按「请假记录不存在」处理。 */
    @Transactional
    public void revoke(Long activityId, Long userId) {
        ActivityLeave existed = leaveMapper.selectOne(new LambdaQueryWrapper<ActivityLeave>()
                .eq(ActivityLeave::getActivityId, activityId)
                .eq(ActivityLeave::getUserId, userId)
                .eq(ActivityLeave::getStatus, 1));
        if (existed == null) {
            throw new IllegalArgumentException("请假记录不存在");
        }
        existed.setStatus(0);
        leaveMapper.updateById(existed);
    }

    /** 该活动生效中的请假列表。 */
    public List<ActivityLeave> activeOf(Long activityId) {
        return leaveMapper.selectList(new LambdaQueryWrapper<ActivityLeave>()
                .eq(ActivityLeave::getActivityId, activityId)
                .eq(ActivityLeave::getStatus, 1)
                .orderByAsc(ActivityLeave::getId));
    }

    /** 某成员在某活动是否有生效请假。 */
    public boolean hasActiveLeave(Long activityId, Long userId) {
        Long n = leaveMapper.selectCount(new LambdaQueryWrapper<ActivityLeave>()
                .eq(ActivityLeave::getActivityId, activityId)
                .eq(ActivityLeave::getUserId, userId)
                .eq(ActivityLeave::getStatus, 1));
        return n != null && n > 0;
    }

    private String trimToNull(String v) {
        if (!StringUtils.hasText(v)) {
            return null;
        }
        return v.trim();
    }
}
