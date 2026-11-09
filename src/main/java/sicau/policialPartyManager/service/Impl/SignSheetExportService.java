package sicau.policialPartyManager.service.Impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sicau.policialPartyManager.api.dto.AttendanceVo;
import sicau.policialPartyManager.utils.ExcelUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 签到名单导出（#71 / #68 补齐）。
 * <p>把考勤统计的三张名单（签到/请假/缺席）拼成一份 xlsx 供线下存档。
 * <p>只读、不做权限判断——调用方（控制器）必须已通过支部范围闸门。
 * <p>建表流程走 {@link ExcelUtil#createSheet}，不再自写一遍 POI。
 */
@Service
@RequiredArgsConstructor
public class SignSheetExportService {

    private final AttendanceServiceImpl attendanceService;

    private static final String[] HEADERS = {"学号", "姓名", "支部", "考勤状态", "签到时间", "请假理由"};

    public byte[] export(Long activityId) {
        AttendanceVo stat = attendanceService.of(activityId);
        List<String[]> rows = new ArrayList<>();
        collect(rows, stat.getSigned());
        collect(rows, stat.getLeave());
        collect(rows, stat.getAbsent());
        return ExcelUtil.createSheet("签到名单", HEADERS, rows);
    }

    private void collect(List<String[]> rows, List<AttendanceVo.Attendee> attendees) {
        if (attendees == null) {
            return;
        }
        for (AttendanceVo.Attendee a : attendees) {
            rows.add(new String[]{
                    a.getStudentId(),
                    a.getRealName(),
                    a.getBranchName(),
                    translate(a.getAttendanceStatus()),
                    a.getSignTime() == null ? "" : a.getSignTime().toString(),
                    a.getLeaveReason()
            });
        }
    }

    /** 状态码 → 中文，导出给人看 */
    private String translate(String status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case AttendanceServiceImpl.SIGNED_NORMAL -> "正常签到";
            case AttendanceServiceImpl.SIGNED_LATE -> "迟到";
            case AttendanceServiceImpl.LEAVE -> "请假";
            case AttendanceServiceImpl.ABSENT -> "缺席";
            default -> status;
        };
    }
}
