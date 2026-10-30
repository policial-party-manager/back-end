package sicau.policialPartyManager.api.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 学习记录视图（内嵌学生姓名与课程名/类型，列表批量回查避免 N+1）。
 */
@Data
public class StudyRecordVo {

    private Long id;

    /** 学生用户id */
    private Long userId;

    /** 学生姓名 */
    private String studentName;

    /** 课程id */
    private Long courseId;

    /** 课程名 */
    private String courseName;

    /** 课程类型 */
    private String courseType;

    /** 学时 */
    private LocalTime studyTime;

    /** 成绩（0~100，两位小数） */
    private BigDecimal score;

    /** 证书链接 */
    private String url;

    /** 备注 */
    private String remark;

    private LocalDateTime createTime;
}
