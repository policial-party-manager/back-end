package sicau.policialPartyManager.service.runner;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import sicau.policialPartyManager.api.dto.UserSaveRequest;
import sicau.policialPartyManager.model.entity.User;
import sicau.policialPartyManager.repository.UserMapper;
import sicau.policialPartyManager.repository.UserRoleMapper;
import sicau.policialPartyManager.service.AdminUserService;

@Component
@Order(1)
public class InitSuperAdminRunner implements ApplicationRunner {

    private final UserMapper userMapper;
    private final AdminUserService adminUserService;

    @Autowired
    public InitSuperAdminRunner(UserMapper userMapper, AdminUserService adminUserService) {
        this.userMapper = userMapper;
        this.adminUserService = adminUserService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 寻找超级管理员用户
        User superAdmin = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, "superadmin"));
        if (superAdmin == null) {
            UserSaveRequest request = new UserSaveRequest();
            request.setUsername("superadmin");
            request.setPassword("123456");
            request.setRealName("超级管理员");
            request.setStudentId("1");
            request.setRoleId(1L);
            adminUserService.createUser(request);
        }
    }
}