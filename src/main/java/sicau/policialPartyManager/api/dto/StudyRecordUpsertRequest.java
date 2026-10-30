package sicau.policialPartyManager.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * 学习记录录入请求（#32，管理员侧）。
 * <p>
 * 一人一课一行：同一 (userId, courseId) 重复提交 = 覆盖更新（triage 决策）。
 * user_id 由请求显式携带（录入对象），不是当前登录用户。
 */
@Data
public class StudyRecordUpsertRequest {

    /** 目标用户id（须有 tb_user_detail 档案） */
    @NotNull(message = "用户id不能为空")
    private Long userId;

    /** 课程id */
    @NotNull(message = "课程id不能为空")
    private Long courseId;

    /** 学时（tb_study_record.study_time time） */
    @NotNull(message = "学时不能为空")
    private LocalTime studyTime;

    /** 成绩 0~100，支持两位小数（decimal(5,2)） */
    @NotNull(message = "成绩不能为空")
    @DecimalMin(value = "0", message = "成绩不能小于 0")
    @DecimalMax(value = "100", message = "成绩不能大于 100")
    private BigDecimal score;

    /** 证书链接（varchar(30)，可空） */
    @Size(max = 30, message = "证书链接长度不能超过 30")
    private String url;

    /** 备注（可空） */
    private String remark;
}
