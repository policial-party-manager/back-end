package sicau.policialPartyManager.api.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限管理-角色 VO：角色 + 已分配权限名 + 使用人数
 */
@Data
public class RoleVo {

    private Long id;

    /** 角色编码（规范化，如 super_admin / branch_admin / student） */
    private String roleCode;

    private String description;

    /** 该角色已分配的权限名 */
    private List<String> permissions = new ArrayList<>();

    /** 使用该角色的用户数 */
    private Long userCount;
}
