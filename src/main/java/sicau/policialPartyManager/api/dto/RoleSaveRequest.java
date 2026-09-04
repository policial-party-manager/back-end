package sicau.policialPartyManager.api.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限管理-角色保存请求
 */
@Data
public class RoleSaveRequest {

    /** 角色编码（如 branch_admin），可为空表示沿用原名 */
    private String roleCode;

    private String description;

    /** 本次分配的权限名集合（全量覆盖） */
    private List<String> permissionNames = new ArrayList<>();
}
