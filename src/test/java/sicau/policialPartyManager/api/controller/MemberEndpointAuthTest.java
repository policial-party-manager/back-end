package sicau.policialPartyManager.api.controller;

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
 * 成员域的安全链行为（#70）。
 * <p>这类断言必须走完整 Spring 安全拦截链：service 单测直接构造对象，
 * 看不见方法级鉴权与认证入口点的行为（#63 的教训）。
 * <p>本类只断言「谁能进门」，不断言业务结果——业务隔离规则见
 * {@code BranchScopeServiceImplTest}。
 */
@SpringBootTest
@AutoConfigureMockMvc
class MemberEndpointAuthTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void branchAdminCanReachMemberList() throws Exception {
        String token = jwtUtil.generateAccessToken(7L, "branch-probe", "branch_admin");
        // 支委可达该端点（未挂支部会得到业务 400，但不是 401/403）
        mockMvc.perform(get("/api/v4/members/page")
                        .header("Authorization", "Bearer " + token))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    if (s == 401 || s == 403) {
                        throw new AssertionError("支委应可进入成员域，实际 " + s);
                    }
                });
    }

    @Test
    void superAdminCanReachMemberList() throws Exception {
        String token = jwtUtil.generateAccessToken(1L, "root-probe", "super_admin");
        mockMvc.perform(get("/api/v4/members/page")
                        .header("Authorization", "Bearer " + token))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    if (s == 401 || s == 403) {
                        throw new AssertionError("超管应可进入成员域，实际 " + s);
                    }
                });
    }

    @Test
    void anonymousIsRejected() throws Exception {
        mockMvc.perform(get("/api/v4/members/page"))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    if (s < 400 || s >= 500) {
                        throw new AssertionError("匿名请求应被拒绝且不得 5xx，实际 " + s);
                    }
                });
    }

    @Test
    void adminOnlyEndpointsStillRejectBranchAdmin() throws Exception {
        String token = jwtUtil.generateAccessToken(7L, "branch-probe", "branch_admin");
        // 超管专属：Excel 导入、批量停用、改所属支部——支委不得进入
        mockMvc.perform(get("/api/v4/admin/users/template")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void studentCannotReachMemberDomain() throws Exception {
        String token = jwtUtil.generateAccessToken(999L, "student-probe", "student");
        mockMvc.perform(get("/api/v4/members/page")
                        .header("Authorization", "Bearer " + token))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    if (s != 403 && s != 400) {
                        throw new AssertionError("普通成员应被拒绝，实际 " + s);
                    }
                });
    }
}
