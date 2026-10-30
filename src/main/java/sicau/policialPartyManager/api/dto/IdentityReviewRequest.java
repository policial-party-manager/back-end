package sicau.policialPartyManager.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 身份变更审批请求（#44，仅 super_admin）。
 */
@Data
public class IdentityReviewRequest {

    /** true=通过（写回 identity_id），false=驳回（不动现身份） */
    @NotNull(message = "请选择通过或驳回")
    private Boolean pass;

    /** 审批意见（tb_identity_change.review_remark varchar(50)，可空） */
    @Size(max = 50, message = "审批意见长度不能超过 50")
    private String remark;
}
