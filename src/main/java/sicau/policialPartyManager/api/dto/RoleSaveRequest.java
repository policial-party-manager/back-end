package sicau.policialPartyManager.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限管理-角色保存请求（新增/编辑共用；roleCode 新增时必填）
 */
@Data
public class RoleSaveRequest {

    /** 角色编码（如 branch_admin），新增必填，编辑可空表示沿用原名 */
    @NotBlank(groups = ValidationGroups.Create.class, message = "角色编码不能为空")
    private String roleCode;

    private String description;

    /** 本次分配的权限名集合（全量覆盖，仅新增时生效；分配请走 /roles/{id}/permissions） */
    private List<String> permissionNames = new ArrayList<>();
}
