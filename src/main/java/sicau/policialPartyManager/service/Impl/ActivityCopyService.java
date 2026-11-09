package sicau.policialPartyManager.service.Impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sicau.policialPartyManager.model.entity.Activity;
import sicau.policialPartyManager.model.entity.ActivityDetail;
import sicau.policialPartyManager.model.entity.ActivityIdentity;
import sicau.policialPartyManager.repository.ActivityDetailMapper;
import sicau.policialPartyManager.repository.ActivityIdentityMapper;
import sicau.policialPartyManager.repository.ActivityMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 复制活动（#68 补齐，票面故事 27）。
 * <p>每月重复的组织生活会不必重填：把上一期的主体、详情、参与范围复制成一份新活动，
 * 时间整体后移一个周期，并置为待发布（status=2 表示已下线/草稿侧），由创建者确认后发布。
 * <p><b>不复制</b>：签到记录、报名记录、请假记录——那些属于「已发生的事实」，
 * 复制会把上一期的到勤情况带进新活动，直接污染统计。
 */
@Service
@RequiredArgsConstructor
public class ActivityCopyService {

    private final ActivityMapper activityMapper;
    private final ActivityDetailMapper activityDetailMapper;
    private final ActivityIdentityMapper activityIdentityMapper;

    /** 默认把时间整体后移 30 天；调用方可指定。 */
    private static final int DEFAULT_SHIFT_DAYS = 30;

    /**
     * 复制活动。
     *
     * @param sourceId 源活动 id（调用方已校验过范围）
     * @param operatorId 新活动的创建者
     * @param shiftDays 时间后移天数；null 用默认 30 天
     * @return 新活动 id
     */
    @Transactional
    public Long copy(Long sourceId, Long operatorId, Integer shiftDays) {
        Activity src = activityMapper.selectById(sourceId);
        if (src == null) {
            throw new IllegalArgumentException("活动不存在");
        }
        int shift = shiftDays == null ? DEFAULT_SHIFT_DAYS : shiftDays;

        Activity copy = new Activity();
        copy.setBranchId(src.getBranchId());
        copy.setTitle(src.getTitle() == null ? "未命名活动" : src.getTitle() + "（复制）");
        copy.setDescription(src.getDescription());
        copy.setType(src.getType());
        copy.setCover(src.getCover());
        copy.setStartTime(shift(src.getStartTime(), shift));
        copy.setEndTime(shift(src.getEndTime(), shift));
        activityMapper.insert(copy);

        ActivityDetail srcDetail = activityDetailMapper.selectById(sourceId);
        ActivityDetail detail = new ActivityDetail();
        detail.setActivityId(copy.getId());
        detail.setCreatorId(operatorId);
        if (srcDetail != null) {
            detail.setLocation(srcDetail.getLocation());
            detail.setLocationDescription(srcDetail.getLocationDescription());
            detail.setSignStart(shift(srcDetail.getSignStart(), shift));
            detail.setSignEnd(shift(srcDetail.getSignEnd(), shift));
            detail.setMaxParticipants(srcDetail.getMaxParticipants());
        }
        // 新活动一律置为未发布：复制出来的是草稿，发布动作由人确认
        detail.setStatus(2);
        activityDetailMapper.insert(detail);

        // 参与范围复制：只复制配置，不带任何人的签到/报名事实
        List<ActivityIdentity> rows = activityIdentityMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ActivityIdentity>()
                        .eq(ActivityIdentity::getActivityId, sourceId));
        for (ActivityIdentity row : rows) {
            ActivityIdentity n = new ActivityIdentity();
            n.setActivityId(copy.getId());
            n.setTargetIdentity(row.getTargetIdentity());
            n.setGrade(row.getGrade());
            n.setCollege(row.getCollege());
            activityIdentityMapper.insert(n);
        }
        return copy.getId();
    }

    private LocalDateTime shift(LocalDateTime t, int days) {
        return t == null ? null : t.plus(Duration.ofDays(days));
    }
}
