package sicau.policialPartyManager.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import sicau.policialPartyManager.api.dto.RoleSaveRequest;
import sicau.policialPartyManager.api.dto.RoleVo;
import sicau.policialPartyManager.model.entity.Role;
import sicau.policialPartyManager.model.entity.RolePermission;
import sicau.policialPartyManager.model.entity.UserRole;
import sicau.policialPartyManager.repository.RoleMapper;
import sicau.policialPartyManager.repository.RolePermissionMapper;
import sicau.policialPartyManager.repository.UserRoleMapper;
import sicau.policialPartyManager.service.AdminRoleService;

import java.util.List;

/**
 * 权限管理（后台）：角色 CRUD、内置权限点字典、角色↔权限名映射维护。
 * <p>
 * 说明：现阶段采用角色级鉴权（接口用 @PreAuthorize(hasRole) 控制），
 * 角色-权限名映射记录在 tb_role_permission，用于维护与后续细粒度鉴权预留。
 */
@Service
@RequiredArgsConstructor
public class AdminRoleServiceImpl implements AdminRoleService {

    /** 内置权限点字典（可分配项） */
    private static final List<String> PERMISSION_NAMES = List.of(
            "user:query", "user:manage",
            "branch:manage",
            "role:manage",
            "activity:manage",
            "news:manage",
            "notice:manage"
    );

    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final UserRoleMapper userRoleMapper;

    @Override
    public List<RoleVo> listRoles() {
        List<Role> roles = roleMapper.selectList(new LambdaQueryWrapper<Role>().orderByAsc(Role::getId));
        return roles.stream().map(this::toVo).toList();
    }

    @Override
    public RoleVo getRole(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new IllegalArgumentException("角色不存在");
        }
        return toVo(role);
    }

    @Override
    public void saveRole(RoleSaveRequest request) {
        String code = trim(request.getRoleCode());
        String roleName = toRoleName(code);
        checkCodeFree(roleName, null);

        Role role = new Role();
        role.setRoleName(roleName);
        role.setDescription(request.getDescription());
        roleMapper.insert(role);

        if (request.getPermissionNames() != null && !request.getPermissionNames().isEmpty()) {
            assignPermissions(role.getId(), request.getPermissionNames());
        }
    }

    @Override
    public void updateRole(Long id, RoleSaveRequest request) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new IllegalArgumentException("角色不存在");
        }
        if (StringUtils.hasText(request.getRoleCode())) {
            String roleName = toRoleName(request.getRoleCode());
            checkCodeFree(roleName, id);
            role.setRoleName(roleName);
        }
        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }
        roleMapper.updateById(role);
    }

    @Override
    public void deleteRole(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            return;
        }
        Long used = userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, id));
        if (used != null && used > 0) {
            throw new IllegalArgumentException("该角色仍被用户使用，无法删除");
        }
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, id));
        roleMapper.deleteById(id);
    }

    @Override
    public List<String> listPermissionNames() {
        return PERMISSION_NAMES;
    }

    @Override
    public void assignPermissions(Long roleId, List<String> permissionNames) {
        if (roleMapper.selectById(roleId) == null) {
            throw new IllegalArgumentException("角色不存在");
        }
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId));
        if (permissionNames == null || permissionNames.isEmpty()) {
            return;
        }
        for (String name : permissionNames) {
            if (!StringUtils.hasText(name)) {
                continue;
            }
            RolePermission rp = new RolePermission();
            rp.setRoleId(roleId);
            rp.setPermissionName(name.trim());
            rolePermissionMapper.insert(rp);
        }
    }

    // ======================= 私有辅助 =======================

    private RoleVo toVo(Role role) {
        RoleVo vo = new RoleVo();
        vo.setId(role.getId());
        vo.setRoleCode(normalizeRole(role.getRoleName()));
        vo.setDescription(role.getDescription());
        List<String> permissions = rolePermissionMapper.selectList(
                        new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, role.getId()))
                .stream().map(RolePermission::getPermissionName).toList();
        vo.setPermissions(permissions);
        Long count = userRoleMapper.selectCount(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, role.getId()));
        vo.setUserCount(count == null ? 0L : count);
        return vo;
    }

    /** 名称转存储格式：统一 "ROLE_" + 大写（兼容 tb_role 的 ROLE_ 前缀约束） */
    private String toRoleName(String code) {
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("角色编码不能为空");
        }
        String c = code.trim();
        if (c.regionMatches(true, 0, "ROLE_", 0, 5)) {
            c = c.substring(5);
        }
        return "ROLE_" + c.toUpperCase();
    }

    /** 存储名转展示编码：去前缀转小写 */
    private String normalizeRole(String roleName) {
        if (!StringUtils.hasText(roleName)) {
            return "";
        }
        String role = roleName.trim();
        if (role.regionMatches(true, 0, "ROLE_", 0, 5)) {
            role = role.substring(5);
        }
        return role.toLowerCase();
    }

    /** 规范化后与其他角色冲突检查（忽略大小写与前缀差异） */
    private void checkCodeFree(String roleName, Long excludeId) {
        List<Role> roles = roleMapper.selectList(null);
        for (Role r : roles) {
            if (excludeId != null && excludeId.equals(r.getId())) {
                continue;
            }
            if (normalizeRole(roleName).equals(normalizeRole(r.getRoleName()))) {
                throw new IllegalArgumentException("角色已存在");
            }
        }
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
