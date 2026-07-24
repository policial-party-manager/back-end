package sicau.policialPartyManager.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import sicau.policialPartyManager.entity.*;
import sicau.policialPartyManager.repository.*;

/**
 * 首次启动时创建测试账号（密码统一为 123456）
 */
@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final IdentityMapper identityMapper;
    private final PasswordEncoder passwordEncoder;

    /** 使用 @Lazy 延迟注入，避免数据库未就绪时阻塞启动 */
    public DataInitializer(@Lazy UserMapper userMapper,
                           @Lazy RoleMapper roleMapper,
                           @Lazy UserRoleMapper userRoleMapper,
                           @Lazy IdentityMapper identityMapper,
                           @Lazy PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.identityMapper = identityMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        try {
            // 初始化角色
            initRoles();
            // 初始化政治身份
            initIdentities();
            // 初始化测试用户
            initUsers();

            log.info("测试账号初始化完成: admin/123456 (super_admin), branch1/123456 (branch_admin), student1/123456 (student)");
        } catch (Exception e) {
            log.warn("测试账号初始化失败（数据库未就绪？）: {}", e.getMessage());
        }
    }

    private void initRoles() {
        createRoleIfNotExists("super_admin", "超级管理员");
        createRoleIfNotExists("branch_admin", "支部管理员");
        createRoleIfNotExists("student", "普通成员");
    }

    private void createRoleIfNotExists(String roleCode, String roleName) {
        Long count = roleMapper.selectCount(
                new LambdaQueryWrapper<Role>().eq(Role::getRoleCode, roleCode));
        if (count == 0) {
            Role role = new Role();
            role.setRoleCode(roleCode);
            role.setRoleName(roleName);
            roleMapper.insert(role);
        }
    }

    private void initIdentities() {
        createIdentityIfNotExists("普通学生", 1);
        createIdentityIfNotExists("入党申请人", 2);
        createIdentityIfNotExists("积极分子", 3);
        createIdentityIfNotExists("发展对象", 4);
        createIdentityIfNotExists("预备党员", 5);
        createIdentityIfNotExists("正式党员", 6);
    }

    private void createIdentityIfNotExists(String name, int level) {
        Long count = identityMapper.selectCount(
                new LambdaQueryWrapper<Identity>().eq(Identity::getIdentityName, name));
        if (count == 0) {
            Identity identity = new Identity();
            identity.setIdentityName(name);
            identity.setLevel(level);
            identityMapper.insert(identity);
        }
    }

    private void initUsers() {
        createUserIfNotExists("admin", "张老师", "super_admin");
        createUserIfNotExists("branch1", "李支委", "branch_admin");
        createUserIfNotExists("student1", "王同学", "student");
    }

    private void createUserIfNotExists(String username, String realName, String roleCode) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (count == 0) {
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode("123456"));
            user.setRealName(realName);
            userMapper.insert(user);

            // 创建用户-角色关联
            Role role = roleMapper.selectOne(
                    new LambdaQueryWrapper<Role>().eq(Role::getRoleCode, roleCode));
            if (role != null) {
                UserRole userRole = new UserRole();
                userRole.setUserId(user.getId());
                userRole.setRoleId(role.getId());
                userRoleMapper.insert(userRole);
            }
        }
    }
}
