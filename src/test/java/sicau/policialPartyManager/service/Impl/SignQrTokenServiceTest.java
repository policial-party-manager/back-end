package sicau.policialPartyManager.service.Impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 签到二维码 token 规则单测（#79）。
 * <p>不 mock 框架类型：本机 JDK 25 下 Byte Buddy 无法 instrument
 * {@code StringRedisTemplate} 的继承链。改为注入 {@link SignQrTokenService.TokenStore}
 * 的内存替身——断言落在真实调用路径上，也顺带证明存储是可替换的。
 */
class SignQrTokenServiceTest {

    private static final Long ACT = 1L;

    private SignQrTokenService service;
    private FakeStore store;

    @BeforeEach
    void setUp() {
        store = new FakeStore();
        service = new SignQrTokenService(null);   // Redis 客户端在替身路径下不会被触碰
        ReflectionTestUtils.setField(service, "ttlSeconds", 60L);
        service.useStore(store);
    }

    @Test
    void issueStoresTokenWithTtl() {
        String token = service.issue(ACT);

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertEquals(token, store.data.get("sign:activity:1"));
        assertEquals(Duration.ofSeconds(60), store.lastTtl);
    }

    @Test
    void issueReusesTokenWithinWindow() {
        store.data.put("sign:activity:1", "existing-token");

        assertEquals("existing-token", service.issue(ACT),
                "同一窗口内应复用现有 token，让投屏二维码稳定");
    }

    @Test
    void verifyAcceptsCurrentToken() {
        store.data.put("sign:activity:1", "abc123");

        assertTrue(service.verify(ACT, "abc123"));
    }

    @Test
    void verifyRejectsStaleOrForgedToken() {
        store.data.put("sign:activity:1", "abc123");

        assertFalse(service.verify(ACT, "old-token"), "过期/伪造 token 必须被拒");
        assertFalse(service.verify(ACT, ""));
        assertFalse(service.verify(ACT, null));
    }

    @Test
    void verifyRejectsWhenNoTokenIssued() {
        assertFalse(service.verify(ACT, "anything"), "未签发时任何 token 都不通过");
    }

    @Test
    void sameTokenUsableByMultipleParticipants() {
        // 同一张二维码可被现场多人扫：verify 幂等，不消耗 token
        store.data.put("sign:activity:1", "shared");

        assertTrue(service.verify(ACT, "shared"));
        assertTrue(service.verify(ACT, "shared"));
        assertTrue(service.verify(ACT, "shared"));
    }

    @Test
    void differentActivitiesHaveIndependentTokens() {
        store.data.put("sign:activity:1", "a");
        store.data.put("sign:activity:2", "b");

        assertTrue(service.verify(1L, "a"));
        assertFalse(service.verify(1L, "b"));
        assertTrue(service.verify(2L, "b"));
    }

    @Test
    void randomTokensDiffer() {
        String t1 = service.issue(1L);
        String t2 = service.issue(2L);

        assertNotEquals(t1, t2, "不同活动签发的 token 不可预测重复");
    }

    @Test
    void expiredTokenNoLongerVerifies() {
        store.data.put("sign:activity:1", "abc123");
        assertTrue(service.verify(1L, "abc123"));

        store.data.remove("sign:activity:1");  // 模拟 TTL 到期

        assertFalse(service.verify(1L, "abc123"), "过期后旧 token 必须失效");
    }

    // ---------- 内存替身 ----------

    private static class FakeStore implements SignQrTokenService.TokenStore {
        final Map<String, String> data = new HashMap<>();
        Duration lastTtl;

        @Override
        public String get(String key) {
            return data.get(key);
        }

        @Override
        public void set(String key, String value, Duration ttl) {
            data.put(key, value);
            lastTtl = ttl;
        }
    }
}
