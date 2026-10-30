package sicau.policialPartyManager.api.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 成员学习汇总（#32）：总时长（分钟）与均分。
 * 学时列是 time 类型，跨行求和以分钟计（hour*60+minute）。
 */
@Data
public class StudySummaryVo {

    /** 记录条数 */
    private long recordCount;

    /** 总学时（分钟） */
    private long totalMinutes;

    /** 平均分（两位小数；无记录为 null） */
    private BigDecimal avgScore;
}
