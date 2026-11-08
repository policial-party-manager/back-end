package sicau.policialPartyManager.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sicau.policialPartyManager.api.dto.AttendanceVo;
import sicau.policialPartyManager.model.entity.ActivityDetail;
import sicau.policialPartyManager.model.entity.ActivityLeave;
import sicau.policialPartyManager.model.entity.Branch;
import sicau.policialPartyManager.model.entity.SignRecord;
import sicau.policialPartyManager.model.entity.UserDetail;
import sicau.policialPartyManager.repository.ActivityDetailMapper;
import sicau.policialPartyManager.repository.ActivityLeaveMapper;
import sicau.policialPartyManager.repository.BranchMapper;
import sicau.policialPartyManager.repository.SignRecordMapper;
import sicau.policialPartyManager.repository.UserDetailMapper;
import sicau.policialPartyManager.service.ActivityScopeService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 活动考勤统计（#78）。
 * <p>考勤四态的判定口径：
 * <ul>
 *   <li><b>正常签到</b>：有签到记录且签到时间 ≤ 签到开始时间</li>
 *   <li><b>迟到</b>：有签到记录且签到时间 &gt; 签到开始时间（查询时推导，不落字段）</li>
 *   <li><b>请假</b>：有生效请假且未签到</li>
 *   <li><b>缺席</b>：在应到范围内，既未签到也未请假</li>
 * </ul>
 * <p>已请假的人到场签到，以签到为准（现场到了就不算请假）。
 * <p>应到 = 参与范围匹配到的全员；活动无范围行时应到不可界定（返回 null），
 * 此时缺席名单为空、参与率为 null——不做猜测性统计。
 */
@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl {

    public static final String SIGNED_NORMAL = "SIGNED_NORMAL";
    public static final String SIGNED_LATE = "SIGNED_LATE";
    public static final String LEAVE = "LEAVE";
    public static final String ABSENT = "ABSENT";

    private final SignRecordMapper signRecordMapper;
    private final ActivityLeaveMapper activityLeaveMapper;
    private final ActivityDetailMapper activityDetailMapper;
    private final UserDetailMapper userDetailMapper;
    private final BranchMapper branchMapper;
    private final ActivityScopeService activityScopeService;

    public AttendanceVo of(Long activityId) {
        List<SignRecord> signs = signRecordMapper.selectList(new LambdaQueryWrapper<SignRecord>()
                .eq(SignRecord::getActivityId, activityId)
                .orderByAsc(SignRecord::getSignTime));
        List<ActivityLeave> leaves = activityLeaveMapper.selectList(new LambdaQueryWrapper<ActivityLeave>()
                .eq(ActivityLeave::getActivityId, activityId)
                .eq(ActivityLeave::getStatus, 1));

        ActivityDetail detail = activityDetailMapper.selectById(activityId);
        LocalDateTime signStart = detail == null ? null : detail.getSignStart();

        Map<Long, SignRecord> signByUser = new HashMap<>();
        for (SignRecord s : signs) {
            signByUser.put(s.getUserId(), s);
        }
        Map<Long, ActivityLeave> leaveByUser = new HashMap<>();
        for (ActivityLeave l : leaves) {
            leaveByUser.put(l.getUserId(), l);
        }

        AttendanceVo vo = new AttendanceVo();
        vo.setActivityId(activityId);

        List<AttendanceVo.Attendee> signedList = new ArrayList<>();
        int late = 0;
        for (SignRecord s : signs) {
            AttendanceVo.Attendee a = toAttendee(s.getUserId());
            a.setSignTime(s.getSignTime());
            boolean isLate = signStart != null && s.getSignTime() != null
                    && s.getSignTime().isAfter(signStart);
            a.setAttendanceStatus(isLate ? SIGNED_LATE : SIGNED_NORMAL);
            if (isLate) {
                late++;
            }
            signedList.add(a);
        }
        vo.setSigned(signedList);
        vo.setActualCount(signs.size());
        vo.setLateCount(late);

        // 请假名单：有生效请假且未签到
        List<AttendanceVo.Attendee> leaveList = new ArrayList<>();
        for (ActivityLeave l : leaves) {
            if (signByUser.containsKey(l.getUserId())) {
                continue; // 到场了，以签到为准
            }
            AttendanceVo.Attendee a = toAttendee(l.getUserId());
            a.setAttendanceStatus(LEAVE);
            a.setLeaveReason(l.getReason());
            leaveList.add(a);
        }
        vo.setLeave(leaveList);
        vo.setLeaveCount(leaveList.size());

        // 应到与缺席
        Integer expected = activityScopeService.expectedCount(activityId);
        vo.setExpectedCount(expected);
        List<AttendanceVo.Attendee> absentList = new ArrayList<>();
        if (expected != null) {
            Set<Long> expectedIds = new LinkedHashSet<>(activityScopeService.expectedUserIds(activityId));
            for (Long uid : expectedIds) {
                if (signByUser.containsKey(uid) || leaveByUser.containsKey(uid)) {
                    continue;
                }
                AttendanceVo.Attendee a = toAttendee(uid);
                a.setAttendanceStatus(ABSENT);
                absentList.add(a);
            }
            vo.setAbsentCount(absentList.size());
            vo.setParticipationRate(expected == 0 ? null
                    : round4(signs.size() * 1.0 / expected));
        } else {
            vo.setAbsentCount(null);
            vo.setParticipationRate(null);
        }
        vo.setAbsent(absentList);
        return vo;
    }

    private AttendanceVo.Attendee toAttendee(Long userId) {
        AttendanceVo.Attendee a = new AttendanceVo.Attendee();
        a.setUserId(userId);
        UserDetail d = userDetailMapper.selectById(userId);
        if (d != null) {
            a.setRealName(d.getName());
            a.setStudentId(d.getStudentId());
            if (d.getBranchId() != null) {
                Branch b = branchMapper.selectById(d.getBranchId());
                a.setBranchName(b == null ? null : b.getBranchName());
            }
        }
        return a;
    }

    private double round4(double v) {
        return Math.round(v * 10000d) / 10000d;
    }
}
