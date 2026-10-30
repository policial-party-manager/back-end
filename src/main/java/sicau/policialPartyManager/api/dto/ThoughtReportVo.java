package sicau.policialPartyManager.api.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 思想汇报列表/详情返回（含提交人与审阅人姓名，便于前端直接展示）。
 */
@Data
public class ThoughtReportVo {

    private Long id;

    /** 提交人用户id */
    private Long userId;

    /** 提交人姓名 */
    private String submitterName;

    /** 审阅人用户id（提交时自动派定的支部书记） */
    private Long reviewerId;

    /** 审阅人姓名 */
    private String reviewerName;

    private String title;

    /** 正文（列表接口不返回，避免大字段；详情返回） */
    private String content;

    /** 附件相对路径，可空 */
    private String file;

    /** 审阅状态：1 待审阅 / 2 通过 / 3 驳回 */
    private Integer status;

    /** 状态中文名 */
    private String statusName;

    private String reviewRemark;

    private LocalDateTime submitTime;

    private LocalDateTime reviewTime;
}
