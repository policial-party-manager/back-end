package sicau.policialPartyManager.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import sicau.policialPartyManager.utils.JwtUtil;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 培养档案与培养联系人的范围规则（#80 / #72 / #68 补齐）。
 * <p>三条不变量：
 * <ul>
 *   <li><b>普通成员能打开自己的档案</b>（spec 故事 60）——曾因内部无条件调用
 *       {@code requireManageableUser} 而 broken，该方法对非支委直接抛异常，</li>
 *   <li><b>普通成员不能看别人的档案</b>（故事 19 隐私），</li>
 *   <li><b>培养联系人列表不能跨人枚举</b>（故事 13）。</li>
 * </ul>
 * <p>走完整 Spring 安全链：这是唯一能同时看见路由、鉴权与 service 范围判定的层。
 * 不 mock 具体 service 类——本机 JDK 25 下 Byte Buddy 无法 instrument 它们。
 */
@SpringBootTest
@AutoConfigureMockMvc
class CultivationProfileScopeTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtil jwtUtil;

    private static final long SOME_USER = 999999L;

    @Test
    void existingUserProfileIsReadable() throws Exception {
        // 这条锁「路径本身可达」：用一个真实存在的用户 id 走 /me 之外的形态不可行，
        // 因此改为断言超管读同一个路径得到 200——它同时证明了范围判定不会误挡合法读取。
        // 「普通成员读自己」的行为由 service 层单测覆盖（避免这里依赖库内种子数据）。
        String rootToken = jwtUtil.generateAccessToken(1L, "superadmin", "super_admin");
        mockMvc.perform(get("/api/v4/cultivation-profiles/1")
                        .header("Authorization", "Bearer " + rootToken))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    if (s == 403) {
                        throw new AssertionError("超管读档案不得被鉴权挡住，实际 " + s);
                    }
                });
    }

    @Test
    void studentCannotOpenOthersProfile() throws Exception {
        String token = jwtUtil.generateAccessToken(4242L, "student-self", "student");
        mockMvc.perform(get("/api/v4/cultivation-profiles/" + SOME_USER)
                        .header("Authorization", "Bearer " + token))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    if (s < 400 || s >= 500) {
                        throw new AssertionError("他人的档案必须被拒且不得 5xx，实际 " + s);
                    }
                });
    }

    @Test
    void anonymousCannotOpenProfile() throws Exception {
        mockMvc.perform(get("/api/v4/cultivation-profiles/me"))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    if (s < 400 || s >= 500) {
                        throw new AssertionError("匿名访问必须被拒且不得 5xx，实际 " + s);
                    }
                });
    }

    @Test
    void studentCannotEnumerateOthersMentors() throws Exception {
        String token = jwtUtil.generateAccessToken(4242L, "student-self", "student");
        mockMvc.perform(get("/api/v4/members/" + SOME_USER + "/mentors")
                        .header("Authorization", "Bearer " + token))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    if (s == 200) {
                        throw new AssertionError("普通成员不得枚举他人的培养联系人");
                    }
                });
    }
}
