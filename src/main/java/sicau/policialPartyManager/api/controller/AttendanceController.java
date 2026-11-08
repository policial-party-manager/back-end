package sicau.policialPartyManager.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sicau.policialPartyManager.api.dto.AttendanceVo;
import sicau.policialPartyManager.api.dto.Result;
import sicau.policialPartyManager.config.CurrentUser;
import sicau.policialPartyManager.model.records.User;
import sicau.policialPartyManager.service.Impl.ActivityLeaveServiceImpl;
import sicau.policialPartyManager.service.Impl.AttendanceServiceImpl;

/**
 * 活动请假与考勤统计（#78）。
 * <p>请假必须落库（有理由、录入人、时间）；迟到由签到时间与签到开始时间推导，不落字段。
 */
@Tag(name = "活动考勤", description = "请假录入/撤销与考勤四态统计")
@RestController
@RequestMapping("/api/v4/admin/activities/{activityId}")
@RequiredArgsConstructor
public class AttendanceController {

    private final ActivityLeaveServiceImpl leaveService;
    private final AttendanceServiceImpl attendanceService;

    @Operation(summary = "录入请假",
            description = "同一成员同一活动只保留一条生效记录；重复录入即更新理由")
    @PostMapping("/leaves")
    public Result<Long> enroll(@CurrentUser User operator, @PathVariable Long activityId,
                               @Valid @RequestBody LeaveRequest request) {
        return Result.ok(leaveService.enroll(activityId, request.getUserId(),
                request.getReason(), operator.userId()));
    }

    @Operation(summary = "撤销请假")
    @DeleteMapping("/leaves/{userId}")
    public Result<Void> revoke(@CurrentUser User operator, @PathVariable Long activityId,
                               @PathVariable Long userId) {
        leaveService.revoke(activityId, userId);
        return Result.ok();
    }

    @Operation(summary = "考勤统计",
            description = "签到/请假/缺席三张名单 + 参与率；考勤四态为正常签到/迟到/请假/缺席。"
                    + "应到不可界定（活动无参与范围）时缺席名单为空、参与率为 null")
    @GetMapping("/attendance")
    public Result<AttendanceVo> attendance(@CurrentUser User operator, @PathVariable Long activityId) {
        return Result.ok(attendanceService.of(activityId));
    }

    @Data
    public static class LeaveRequest {
        @NotNull(message = "请假人不能为空")
        private Long userId;
        @Size(max = 200, message = "请假理由长度不能超过 200")
        private String reason;
    }
}
