package sicau.policialPartyManager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import sicau.policialPartyManager.dto.LoginRequest;
import sicau.policialPartyManager.dto.LoginResponse;
import sicau.policialPartyManager.dto.MenuVo;
import sicau.policialPartyManager.entity.Branch;
import sicau.policialPartyManager.entity.User;
import sicau.policialPartyManager.repository.BranchMapper;
import sicau.policialPartyManager.repository.UserMapper;
import sicau.policialPartyManager.security.JwtUtil;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final BranchMapper branchMapper;
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

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        String branchName = null;
        if (user.getBranchId() != null) {
            Branch branch = branchMapper.selectById(user.getBranchId());
            if (branch != null) branchName = branch.getBranchName();
        }

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .role(user.getRole())
                .branchId(user.getBranchId())
                .branchName(branchName)
                .menus(buildMenus(user.getRole()))
                .build();
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
