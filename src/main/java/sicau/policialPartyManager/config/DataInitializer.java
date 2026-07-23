package sicau.policialPartyManager.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import sicau.policialPartyManager.entity.User;
import sicau.policialPartyManager.repository.UserMapper;

/**
 * 首次启动时创建测试账号（密码统一为 123456）
 */
@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    /** 使用 @Lazy 延迟注入，避免数据库未就绪时阻塞启动 */
    public DataInitializer(@Lazy UserMapper userMapper,
                           @Lazy PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        try {
            createIfNotExists("admin", "张老师", "super_admin", null);
            createIfNotExists("branch1", "李支委", "branch_admin", 1L);
            createIfNotExists("student1", "王同学", "student", 1L);
            log.info("测试账号初始化完成: admin/123456, branch1/123456, student1/123456");
        } catch (Exception e) {
            log.warn("测试账号初始化失败（数据库未就绪？）: {}", e.getMessage());
        }
    }

    private void createIfNotExists(String username, String realName, String role, Long branchId) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (count == 0) {
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode("123456"));
            user.setRealName(realName);
            user.setRole(role);
            user.setBranchId(branchId);
            userMapper.insert(user);
        }
    }
}
