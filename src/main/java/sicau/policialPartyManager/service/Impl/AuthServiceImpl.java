package sicau.policialPartyManager.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import sicau.policialPartyManager.dto.LoginRequest;
import sicau.policialPartyManager.dto.LoginResponse;
import sicau.policialPartyManager.dto.MenuVo;
import sicau.policialPartyManager.entity.*;
import sicau.policialPartyManager.repository.*;
import sicau.policialPartyManager.security.JwtUtil;
import sicau.policialPartyManager.service.AuthService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final BranchMapper branchMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserDetailMapper userDetailMapper;

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (user == null) {
            throw new IllegalArgumentException("用户名不存在");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (user.getStatus() != 1) {
            throw new IllegalArgumentException("账号已被停用");
        }

        // 从 RBAC 关联表获取角色
        String role = getUserRole(user.getId());

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), role);

        // 从 member 表反查用户所属支部
        Long branchId = null;
        String branchName = null;
        UserDetail userDetail = userDetailMapper.selectById(user.getId());
        if (userDetail.getBranchId() != null) {
            Branch branch = branchMapper.selectById(userDetail.getBranchId());
            if (branch == null) {
                throw new RuntimeException("改用户党支部无法查询");
            }
            branchName = branch.getBranchName();
        }

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .realName(userDetail.getName())
                .role(role)
                .branchId(branchId)
                .branchName(branchName)
                .menus(buildMenus(role))
                .build();
    }

    /** 通过 tb_user_role + tb_role 获取用户角色编码 */
    @Override
    public String getUserRole(Long userId) {
        UserRole userRole = userRoleMapper.selectOne(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        if (userRole == null) return "student";
        Role role = roleMapper.selectById(userRole.getRoleId());
        return role != null ? role.getRoleName() : "student";
    }

    @Override
    public List<MenuVo> buildMenus(String role) {
        return switch (role) {
            case "super_admin" -> List.of(
                    MenuVo.builder().name("首页仪表盘").path("/dashboard").icon("HomeFilled").build(),
                    MenuVo.builder().name("成员管理").path("/members").icon("UserFilled").build(),
                    MenuVo.builder().name("党支部管理").path("/branches").icon("OfficeBuilding").build(),
                    MenuVo.builder().name("个人中心").path("/profile").icon("Setting").build()
            );
            case "branch_admin" -> List.of(
                    MenuVo.builder().name("首页仪表盘").path("/dashboard").icon("HomeFilled").build(),
                    MenuVo.builder().name("成员管理").path("/members").icon("UserFilled").build(),
                    MenuVo.builder().name("个人中心").path("/profile").icon("Setting").build()
            );
            default -> List.of(
                    MenuVo.builder().name("首页").path("/dashboard").icon("HomeFilled").build(),
                    MenuVo.builder().name("个人中心").path("/profile").icon("Setting").build()
            );
        };
    }
}
