package sicau.policialPartyManager.service.Impl;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import sicau.policialPartyManager.api.dto.AttendanceVo;

import java.io.ByteArrayOutputStream;

/**
 * 签到名单导出（#71 / #68 补齐）。
 * <p>把考勤统计的三张名单（签到/请假/缺席）拼成一份 xlsx 供线下存档。
 * <p>只读、不做权限判断——调用方（控制器）必须已通过支部范围闸门。
 */
@Service
@RequiredArgsConstructor
public class SignSheetExportService {

    private final AttendanceServiceImpl attendanceService;

    private static final String[] HEADERS = {"学号", "姓名", "支部", "考勤状态", "签到时间", "请假理由"};

    public byte[] export(Long activityId) {
        AttendanceVo stat = attendanceService.of(activityId);
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("签到名单");
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
            }
            int r = 1;
            r = write(sheet, r, stat.getSigned());
            r = write(sheet, r, stat.getLeave());
            write(sheet, r, stat.getAbsent());
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("导出签到名单失败", e);
        }
    }

    private int write(Sheet sheet, int startRow, java.util.List<AttendanceVo.Attendee> rows) {
        int r = startRow;
        if (rows == null) {
            return r;
        }
        for (AttendanceVo.Attendee a : rows) {
            Row row = sheet.createRow(r++);
            set(row, 0, a.getStudentId());
            set(row, 1, a.getRealName());
            set(row, 2, a.getBranchName());
            set(row, 3, translate(a.getAttendanceStatus()));
            set(row, 4, a.getSignTime() == null ? "" : a.getSignTime().toString());
            set(row, 5, a.getLeaveReason());
        }
        return r;
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

    private void set(Row row, int idx, String v) {
        Cell c = row.createCell(idx);
        c.setCellValue(v == null ? "" : v);
    }
}
