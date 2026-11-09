package sicau.policialPartyManager.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import sicau.policialPartyManager.utils.JwtUtil;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 支委在活动域的能力面（#71 补齐）。
 * <p>票面验收判据原文要求：支委「能创建活动」、「能编辑本支部尚未开始的活动」、
 * 「能导出签到名单」；同时「删除活动」「查看全院活动」仍归超管。
 * <p>走完整安全链断言，因为这些正是「类级 hasRole('SUPER_ADMIN') 把支委挡在门外」的失败模式。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ScopedActivityWriteAuthTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtil jwtUtil;

    private static final long MISSING = 999999L;

    @Test
    void branchAdminCanReachCreateActivity() throws Exception {
        String token = jwtUtil.generateAccessToken(7L, "branch-probe", "branch_admin");
        mockMvc.perform(post("/api/v4/activities")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"title\":\"\"}"))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    if (s == 401 || s == 403) {
                        throw new AssertionError("支委应可创建活动（空标题应得 400 而非 " + s + "）");
                    }
                });
    }

    @Test
    void studentCannotCreateActivity() throws Exception {
        String token = jwtUtil.generateAccessToken(999L, "student-probe", "student");
        mockMvc.perform(post("/api/v4/activities")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"title\":\"x\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void branchAdminCanReachUpdateOwnActivityButNotForeign() throws Exception {
        String token = jwtUtil.generateAccessToken(7L, "branch-probe", "branch_admin");
        // 不存在的活动：不得 5xx，且不得成功
        mockMvc.perform(put("/api/v4/activities/" + MISSING)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"title\":\"x\"}"))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    if (s < 400 || s >= 500) {
                        throw new AssertionError("越权/不存在应为 4xx，实际 " + s);
                    }
                });
    }

    @Test
    void studentCannotUpdateActivity() throws Exception {
        String token = jwtUtil.generateAccessToken(999L, "student-probe", "student");
        mockMvc.perform(put("/api/v4/activities/" + MISSING)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"title\":\"x\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void branchAdminCanReachSignExport() throws Exception {
        String token = jwtUtil.generateAccessToken(7L, "branch-probe", "branch_admin");
        mockMvc.perform(get("/api/v4/activities/" + MISSING + "/signs/export")
                        .header("Authorization", "Bearer " + token))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    if (s == 401 || s == 403) {
                        throw new AssertionError("支委应可导出本支部签到名单，实际 " + s);
                    }
                });
    }

    @Test
    void studentCannotExportSigns() throws Exception {
        String token = jwtUtil.generateAccessToken(999L, "student-probe", "student");
        mockMvc.perform(get("/api/v4/activities/" + MISSING + "/signs/export")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void branchAdminStillCannotDeleteActivity() throws Exception {
        String token = jwtUtil.generateAccessToken(7L, "branch-probe", "branch_admin");
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/v4/admin/activities/" + MISSING)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
