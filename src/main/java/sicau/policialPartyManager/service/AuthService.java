package sicau.policialPartyManager.service;

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

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final BranchMapper branchMapper;
    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (user == null) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new IllegalArgumentException("账号已被停用");
        }

        // 从 RBAC 关联表获取角色
        String role = getUserRole(user.getId());

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), role);

        // 从 member 表反查用户所属支部
        String branchName = null;
        Long branchId = null;
        Member member = memberMapper.selectOne(
                new LambdaQueryWrapper<Member>().eq(Member::getStudentNo, user.getUsername()));
        if (member != null && member.getBranchId() != null) {
            branchId = member.getBranchId();
            Branch branch = branchMapper.selectById(branchId);
            if (branch != null) branchName = branch.getBranchName();
        }

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .role(role)
                .branchId(branchId)
                .branchName(branchName)
                .menus(buildMenus(role))
                .build();
    }

    /** 通过 tb_user_role + tb_role 获取用户角色编码 */
    private String getUserRole(Long userId) {
        UserRole userRole = userRoleMapper.selectOne(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        if (userRole == null) return "student";
        Role role = roleMapper.selectById(userRole.getRoleId());
        return role != null ? role.getRoleCode() : "student";
    }

    private List<MenuVo> buildMenus(String role) {
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
