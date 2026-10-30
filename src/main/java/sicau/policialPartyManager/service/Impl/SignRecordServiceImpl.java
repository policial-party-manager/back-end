package sicau.policialPartyManager.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.api.dto.SignInRequest;
import sicau.policialPartyManager.model.entity.Activity;
import sicau.policialPartyManager.model.entity.ActivityDetail;
import sicau.policialPartyManager.model.entity.ActivityType;
import sicau.policialPartyManager.model.entity.SignRecord;
import sicau.policialPartyManager.repository.ActivityDetailMapper;
import sicau.policialPartyManager.repository.ActivityMapper;
import sicau.policialPartyManager.repository.ActivityTypeMapper;
import sicau.policialPartyManager.repository.SignRecordMapper;
import sicau.policialPartyManager.service.SignRecordService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SignRecordServiceImpl implements SignRecordService {

    private final SignRecordMapper signRecordMapper;
    private final ActivityMapper activityMapper;
    private final ActivityDetailMapper activityDetailMapper;
    private final ActivityTypeMapper activityTypeMapper;

    @Override
    @Transactional
    public SignRecord sign(SignInRequest request, Long userId) {
        Long activityId = request.getActivityId();
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new IllegalArgumentException("活动不存在");
        }
        ActivityDetail detail = activityDetailMapper.selectById(activityId);
        if (detail == null || detail.getStatus() == null || detail.getStatus() != 1) {
            throw new IllegalArgumentException("活动未发布，无法签到");
        }

        // 签到窗：两边界都可空，为空视为不限
        LocalDateTime now = LocalDateTime.now();
        if (detail.getSignStart() != null && now.isBefore(detail.getSignStart())) {
            throw new IllegalArgumentException("签到尚未开始");
        }
        if (detail.getSignEnd() != null && now.isAfter(detail.getSignEnd())) {
            throw new IllegalArgumentException("签到已结束");
        }

        // 活动类型开关：need_sign=0 拒绝
        if (activity.getType() != null) {
            ActivityType type = activityTypeMapper.selectById(Long.valueOf(activity.getType()));
            if (type != null && Integer.valueOf(0).equals(type.getNeedSign())) {
                throw new IllegalArgumentException("该活动无需签到");
            }
        }

        // 重复签到（应用层先查 + 数据库唯一约束兜底）
        Long existing = signRecordMapper.selectCount(new LambdaQueryWrapper<SignRecord>()
                .eq(SignRecord::getActivityId, activityId)
                .eq(SignRecord::getUserId, userId));
        if (existing != null && existing > 0) {
            throw new IllegalArgumentException("您已签到，请勿重复签到");
        }

        SignRecord record = new SignRecord();
        record.setActivityId(activityId);
        record.setUserId(userId);
        record.setSignTime(now);
        record.setSignType("sign");
        record.setDevice(trimToNull(request.getDevice()));
        record.setLocation(trimToNull(request.getLocation()));
        record.setRemark(trimToNull(request.getRemark()));
        try {
            signRecordMapper.insert(record);
        } catch (DuplicateKeyException e) {
            // 并发下的兜底路径
            throw new IllegalArgumentException("您已签到，请勿重复签到");
        }
        return record;
    }

    @Override
    public PageResult<SignRecord> pageByActivity(long page, long size, Long activityId, Long userId) {
        if (activityId == null) {
            throw new IllegalArgumentException("活动id不能为空");
        }
        LambdaQueryWrapper<SignRecord> wrapper = new LambdaQueryWrapper<SignRecord>()
                .eq(SignRecord::getActivityId, activityId)
                .orderByDesc(SignRecord::getSignTime);
        if (userId != null) {
            wrapper.eq(SignRecord::getUserId, userId);
        }
        Page<SignRecord> result = signRecordMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
