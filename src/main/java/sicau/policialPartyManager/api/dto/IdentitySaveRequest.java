package sicau.policialPartyManager.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 政治身份字典新增/编辑请求（#44，超管维护）。
 */
@Data
public class IdentitySaveRequest {

    /** 身份名（tb_identity.identity_name varchar(20)，全表唯一） */
    @NotBlank(message = "身份名不能为空")
    @Size(max = 20, message = "身份名长度不能超过 20")
    private String identityName;

    /** 发展阶段（1~6，README「政治身份级别」为权威定义） */
    @NotNull(message = "发展阶段不能为空")
    @Min(value = 1, message = "发展阶段须在 1~6 之间")
    @Max(value = 6, message = "发展阶段须在 1~6 之间")
    private Integer level;

    /**
     * 对应角色id。仅展示映射，不参与鉴权（#44 决策红线：身份变更永不联动登录角色）。
     */
    private Long roleId;

    /** 说明 */
    private String description;
}
