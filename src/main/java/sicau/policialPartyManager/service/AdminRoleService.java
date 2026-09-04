package sicau.policialPartyManager.service;

import sicau.policialPartyManager.api.dto.RoleSaveRequest;
import sicau.policialPartyManager.api.dto.RoleVo;

import java.util.List;

public interface AdminRoleService {

    /** 全部角色（含权限名与使用人数），供角色列表/下拉使用 */
    List<RoleVo> listRoles();

    /** 角色详情（含权限名） */
    RoleVo getRole(Long id);

    /** 新增角色（roleCode 规范化存储为 ROLE_ 前缀大写） */
    void saveRole(RoleSaveRequest request);

    /** 编辑角色（角色编码+描述，并全量覆盖权限名） */
    void updateRole(Long id, RoleSaveRequest request);

    /** 删除角色（仍有用户使用则拒绝） */
    void deleteRole(Long id);

    /** 内置权限点字典（可分配项，现阶段用于角色-权限映射维护/预留） */
    List<String> listPermissionNames();

    /** 全量覆盖角色的权限名（写 tb_role_permission） */
    void assignPermissions(Long roleId, List<String> permissionNames);
}
