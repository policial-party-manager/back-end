package sicau.policialPartyManager.api.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 活动考勤统计（#78）：签到 / 请假 / 缺席 三张名单 + 参与率，
 * 考勤四态为 正常签到 / 迟到 / 请假 / 缺席。
 */
@Data
public class AttendanceVo {

    private Long activityId;

    /** 应到人数；活动无参与范围时为 null（全场开放，应到不可界定） */
    private Integer expectedCount;

    /** 实到人数（有签到记录的人） */
    private Integer actualCount;

    /** 请假人数（生效请假且未签到） */
    private Integer leaveCount;

    /** 缺席人数（应到范围内、既未签到也未请假）；应到不可界定时为 null */
    private Integer absentCount;

    /** 迟到人数（签到时间晚于签到开始时间） */
    private Integer lateCount;

    /** 参与率 = 实到 / 应到；应到为 0 或不可界定时为 null */
    private Double participationRate;

    private List<Attendee> signed;

    private List<Attendee> leave;

    /** 缺席名单；应到不可界定时为空列表 */
    private List<Attendee> absent;

    /** 考勤状态：SIGNED_NORMAL / SIGNED_LATE / LEAVE / ABSENT */
    @Data
    public static class Attendee {
        private Long userId;
        private String realName;
        private String studentId;
        private String branchName;
        /** 考勤状态 */
        private String attendanceStatus;
        private LocalDateTime signTime;
        /** 请假理由（仅请假条目） */
        private String leaveReason;
    }
}
