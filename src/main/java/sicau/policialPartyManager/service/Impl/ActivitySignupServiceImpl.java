package sicau.policialPartyManager.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.model.entity.Activity;
import sicau.policialPartyManager.model.entity.ActivityDetail;
import sicau.policialPartyManager.model.entity.ActivityIdentity;
import sicau.policialPartyManager.model.entity.ActivitySignup;
import sicau.policialPartyManager.model.entity.UserDetail;
import sicau.policialPartyManager.repository.ActivityDetailMapper;
import sicau.policialPartyManager.repository.ActivityIdentityMapper;
import sicau.policialPartyManager.repository.ActivityMapper;
import sicau.policialPartyManager.repository.ActivitySignupMapper;
import sicau.policialPartyManager.repository.UserDetailMapper;
import sicau.policialPartyManager.service.ActivitySignupService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivitySignupServiceImpl implements ActivitySignupService {

    private final ActivitySignupMapper signupMapper;
    private final ActivityMapper activityMapper;
    private final ActivityDetailMapper activityDetailMapper;
    private final ActivityIdentityMapper activityIdentityMapper;
    private final UserDetailMapper userDetailMapper;

    @Override
    @Transactional
    public ActivitySignup signup(Long activityId, Long userId) {
        Activity activity = requireNotStartedPublishedActivity(activityId);

        // 范围校验（年级/学院；身份维度本期不校验，见 #44）
        checkEligibility(activityId, userId);

        // 重复报名：应用层先查 + 唯一约束兜底
        Long existing = signupMapper.selectCount(new LambdaQueryWrapper<ActivitySignup>()
                .eq(ActivitySignup::getActivityId, activityId)
                .eq(ActivitySignup::getUserId, userId));
        if (existing != null && existing > 0) {
            throw new IllegalArgumentException("您已报名，请勿重复报名");
        }

        // 名额：maxParticipants 为空 = 不限
        ActivityDetail detail = activityDetailMapper.selectById(activityId);
        if (detail != null && detail.getMaxParticipants() != null) {
            Long signed = signupMapper.selectCount(new LambdaQueryWrapper<ActivitySignup>()
                    .eq(ActivitySignup::getActivityId, activityId));
            if (signed != null && signed >= detail.getMaxParticipants()) {
                throw new IllegalArgumentException("活动名额已满");
            }
        }

        ActivitySignup record = new ActivitySignup();
        record.setActivityId(activityId);
        record.setUserId(userId);
        record.setSignupTime(LocalDateTime.now());
        try {
            signupMapper.insert(record);
        } catch (DuplicateKeyException e) {
            throw new IllegalArgumentException("您已报名，请勿重复报名");
        }
        // signup_time 服务端填充（DDL 默认值兜底），免去回读，返回值即完整
        return record;
    }

    @Override
    public void cancel(Long activityId, Long userId) {
        requireNotStartedPublishedActivity(activityId);
        ActivitySignup mine = signupMapper.selectOne(new LambdaQueryWrapper<ActivitySignup>()
                .eq(ActivitySignup::getActivityId, activityId)
                .eq(ActivitySignup::getUserId, userId));
        if (mine == null) {
            throw new IllegalArgumentException("您未报名该活动");
        }
        signupMapper.deleteById(mine.getId());
    }

    @Override
    public PageResult<ActivitySignup> pageByActivity(long page, long size, Long activityId, Long userId) {
        if (activityId == null) {
            throw new IllegalArgumentException("活动id不能为空");
        }
        LambdaQueryWrapper<ActivitySignup> wrapper = new LambdaQueryWrapper<ActivitySignup>()
                .eq(ActivitySignup::getActivityId, activityId)
                .orderByDesc(ActivitySignup::getSignupTime);
        if (userId != null) {
            wrapper.eq(ActivitySignup::getUserId, userId);
        }
        Page<ActivitySignup> result = signupMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    /** 活动存在 + 已发布 + 未开始（start_time 空视为未开始） */
    private Activity requireNotStartedPublishedActivity(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new IllegalArgumentException("活动不存在");
        }
        ActivityDetail detail = activityDetailMapper.selectById(activityId);
        if (detail == null || detail.getStatus() == null || detail.getStatus() != 1) {
            throw new IllegalArgumentException("活动未发布，无法报名");
        }
        if (activity.getStartTime() != null && LocalDateTime.now().isAfter(activity.getStartTime())) {
            throw new IllegalArgumentException("活动已开始，不能报名或取消");
        }
        return activity;
    }

    /** 命中任意一行目标人群即可；未配置行 = 全场开放。学院为空=不限；年级按字符串相等比较。 */
    private void checkEligibility(Long activityId, Long userId) {
        List<ActivityIdentity> rows = activityIdentityMapper.selectList(
                new LambdaQueryWrapper<ActivityIdentity>().eq(ActivityIdentity::getActivityId, activityId));
        if (rows == null || rows.isEmpty()) {
            return;
        }
        UserDetail user = userDetailMapper.selectById(userId);
        if (user != null) {
            String userGrade = trimOrEmpty(user.getGrade());
            String userCollege = trimOrEmpty(user.getCollege());
            for (ActivityIdentity row : rows) {
                boolean collegeOk = !StringUtils.hasText(row.getCollege())
                        || row.getCollege().trim().equals(userCollege);
                boolean gradeOk = row.getGrade() == null
                        || String.valueOf(row.getGrade()).equals(userGrade);
                if (collegeOk && gradeOk) {
                    return;
                }
            }
        }
        throw new IllegalArgumentException("您不符合该活动的报名范围（年级/学院）");
    }

    private String trimOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
