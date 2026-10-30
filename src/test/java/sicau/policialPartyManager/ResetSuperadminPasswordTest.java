package sicau.policialPartyManager;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 维护任务（可重复执行，故意不加 @Transactional 以真落库）：把 dev 库 superadmin 密码重置为默认 123456，供 scripts/e2e-acceptance.sh 使用。
 * 何时跑：E2E 报「无法登录」且 dev 库凭据已被人工改密时。
 * 默认 @Disabled：不进全量套件（防止每次 mvn test 改写 dev 凭据）。
 * 手动执行：
 *   mvn test -Dtest=ResetSuperadminPasswordTest  *     -Djunit.jupiter.conditions.deactivate=org.junit.*DisabledCondition
 */
@SpringBootTest
@Disabled("维护命令：仅在 dev 库 superadmin 密码漂移时手动执行，见类注释")
class ResetSuperadminPasswordTest {

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void resetDevPassword() {
        int n = jdbc.update("update tb_user set password = ? where username = 'superadmin'",
                passwordEncoder.encode("123456"));
        assertTrue(n >= 1, "superadmin 行不存在？");
        System.out.println("[maint] superadmin password reset rows=" + n);
    }
}
