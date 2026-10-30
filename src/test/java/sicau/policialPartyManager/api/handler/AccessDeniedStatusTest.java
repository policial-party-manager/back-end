package sicau.policialPartyManager.api.handler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import sicau.policialPartyManager.utils.JwtUtil;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * #63 回归：@PreAuthorize 拒绝必须得到 HTTP 403 + 统一响应体，
 * 而不是掉进 catch-all 变 500「服务器内部错误」。
 * <p>
 * 这是唯一能抓住该 bug 的测试层：必须走完整 Spring 安全拦截链
 * （service/契约测试直接构造对象，永远看不见方法级鉴权的行为）。
 */
@SpringBootTest
@AutoConfigureMockMvc
class AccessDeniedStatusTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void lowRoleOnAdminEndpointIs403Not500() throws Exception {
        String studentToken = jwtUtil.generateAccessToken(999L, "e2e-role-probe", "student");

        mockMvc.perform(get("/api/v4/admin/study-records/page")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void anonymousOnAdminEndpointIsUnauthorized() throws Exception {
        // 无 token：认证入口点行为在 SecurityConfig 有交代（401/403 均合法，
        // 唯独不允许 500）。断言只锁「不是 5xx」。
        mockMvc.perform(get("/api/v4/admin/study-records/page"))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    if (s >= 500) {
                        throw new AssertionError("匿名请求不得变 5xx，实际 " + s);
                    }
                });
    }

    @Test
    void businessValidationErrorStill400() throws Exception {
        // catch-all 收窄后，既有 400 语义（服务层校验）不得回归
        String studentToken = jwtUtil.generateAccessToken(999L, "e2e-role-probe", "student");
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v4/thought-reports")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}
