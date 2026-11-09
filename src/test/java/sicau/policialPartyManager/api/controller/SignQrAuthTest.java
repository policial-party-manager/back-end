package sicau.policialPartyManager.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import sicau.policialPartyManager.utils.JwtUtil;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 二维码签到的越权回归（#68 补齐）。
 * <p>两个必须锁死的行为：
 * <ul>
 *   <li><b>生成二维码只有管理侧能调</b>——普通成员若能生成，就能自己造码绕开现场签到；</li>
 *   <li><b>生成二维码限本支部</b>——支委不得为其他支部的活动签发签到码。</li>
 * </ul>
 * <p>走完整 Spring 安全链：service 单测看不见鉴权行为（#63 的教训）。
 */
@SpringBootTest
@AutoConfigureMockMvc
class SignQrAuthTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtil jwtUtil;

    private static final long ANY_ACTIVITY = 999999L;

    @Test
    void studentCannotIssueQr() throws Exception {
        String token = jwtUtil.generateAccessToken(999L, "student-probe", "student");
        mockMvc.perform(get("/api/v4/activities/" + ANY_ACTIVITY + "/sign-qr")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void anonymousCannotIssueQr() throws Exception {
        mockMvc.perform(get("/api/v4/activities/" + ANY_ACTIVITY + "/sign-qr"))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    if (s < 400 || s >= 500) {
                        throw new AssertionError("匿名签发二维码必须被拒且不得 5xx，实际 " + s);
                    }
                });
    }

    @Test
    void branchAdminCannotIssueQrForForeignOrMissingActivity() throws Exception {
        // 支委能过安全链，但活动不在其支部（该 id 不存在）→ 不存在语义
        String token = jwtUtil.generateAccessToken(7L, "branch-probe", "branch_admin");
        mockMvc.perform(get("/api/v4/activities/" + ANY_ACTIVITY + "/sign-qr")
                        .header("Authorization", "Bearer " + token))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    if (s == 200) {
                        throw new AssertionError("支委不得为不属于本支部的活动签发二维码");
                    }
                });
    }

    @Test
    void studentCannotSignByQrOnForeignActivity() throws Exception {
        // 扫码签到本身对普通成员开放，但必须先通过 token 校验；
        // 这里断言的是「未持有效 token 不会写入签到」。
        String token = jwtUtil.generateAccessToken(999L, "student-probe", "student");
        mockMvc.perform(post("/api/v4/activities/" + ANY_ACTIVITY + "/sign-by-qr")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"token\":\"forged\"}"))
                .andExpect(status().isBadRequest());
    }
}
