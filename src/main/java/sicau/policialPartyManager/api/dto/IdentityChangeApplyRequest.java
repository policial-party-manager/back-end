package sicau.policialPartyManager.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 身份变更发起请求（#44，两阶段的第一步）。
 * <p>
 * fromIdentity 不在请求内：由服务端取目标用户当前 identity_id 快照（首次登记为 null）。
 * 变更生效仅写回身份列，绝不联动登录角色。
 */
@Data
public class IdentityChangeApplyRequest {

    /** 目标用户id（须有成员档案；branch_admin 限本支部） */
    @NotNull(message = "用户id不能为空")
    private Long userId;

    /** 变更后身份（tb_identity.id） */
    @NotNull(message = "目标身份不能为空")
    private Long toIdentity;

    /** 变更原因（可空） */
    @Size(max = 500, message = "变更原因长度不能超过 500")
    private String reason;
}
