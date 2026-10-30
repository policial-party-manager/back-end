package sicau.policialPartyManager.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 思想汇报审阅请求（审阅人侧）。
 */
@Data
public class ThoughtReviewRequest {

    /** true=通过（status 2），false=驳回（status 3） */
    @NotNull(message = "请选择通过或驳回")
    private Boolean pass;

    /** 审阅意见（tb_thought_report.review_remark varchar(50)，可空） */
    @Size(max = 50, message = "审阅意见长度不能超过 50")
    private String remark;
}
