package sicau.policialPartyManager.service.Impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

/**
 * 签到二维码 token（#79）。
 * <p>token 存 Redis：键 {@code sign:activity:{activityId}}，值为随机不可预测串，
 * TTL 由配置项 {@code app.sign.qr-ttl} 控制（默认 60 秒）。过期后旧 token 立即失效——
 * 这是「防截图转发」的唯一实现方式。
 * <p><b>一个有效 token 可被该活动的所有参与者使用</b>，不是一次性凭证：
 * token 证明的是「此刻在场」，不是「谁抢到了」。防重复靠 {@code (activity_id, user_id)} 唯一约束。
 * <p>Redis 不可用时降级为进程内存储，保证单机开发环境仍可演示，但时效只在本进程内有效。
 * <p>存储读写通过 {@link TokenStore} 这一窄接口隔离：既让本服务可单测（不受本机
 * JDK 对 {@code StringRedisTemplate} 继承链的 mock 限制），也不把 Redis 细节漏给调用方。
 */
@Service
@RequiredArgsConstructor
public class SignQrTokenService {

    private static final String KEY_PREFIX = "sign:activity:";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;

    /** token 有效期（秒），默认 60 */
    @Value("${app.sign.qr-ttl:60}")
    private long ttlSeconds;

    /** 进程内降级存储：activityId -> token */
    private final java.util.Map<Long, String> fallbackTokens = new java.util.concurrent.ConcurrentHashMap<>();

    /** 读写窄接口：便于单测替换，不暴露 Redis API。 */
    public interface TokenStore {
        String get(String key);

        void set(String key, String value, Duration ttl);
    }

    /** 默认实现：走 Redis；Redis 异常时抛出由调用方降级。 */
    private TokenStore redisStore() {
        return new TokenStore() {
            @Override
            public String get(String key) {
                return redisTemplate.opsForValue().get(key);
            }

            @Override
            public void set(String key, String value, Duration ttl) {
                redisTemplate.opsForValue().set(key, value, ttl);
            }
        };
    }

    /** 供测试注入替身。 */
    void useStore(TokenStore store) {
        this.override = store;
    }

    private TokenStore override;

    private TokenStore store() {
        return override != null ? override : redisStore();
    }

    /**
     * 签发当前有效 token；已存在未过期的 token 时复用（保证投屏上的二维码在同一窗口内稳定）。
     *
     * @return 当前 token
     */
    public String issue(Long activityId) {
        String key = KEY_PREFIX + activityId;
        try {
            String existed = store().get(key);
            if (StringUtils.hasText(existed)) {
                return existed;
            }
            String token = randomToken();
            store().set(key, token, Duration.ofSeconds(ttlSeconds));
            return token;
        } catch (RuntimeException e) {
            // Redis 不可用：降级为进程内 token
            String token = fallbackTokens.getOrDefault(activityId, randomToken());
            fallbackTokens.put(activityId, token);
            return token;
        }
    }

    /** 当前有效 token；无则返回 null。 */
    public String current(Long activityId) {
        String key = KEY_PREFIX + activityId;
        try {
            return store().get(key);
        } catch (RuntimeException e) {
            return fallbackTokens.get(activityId);
        }
    }

    /** 校验 token 是否当前有效。 */
    public boolean verify(Long activityId, String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        String currentToken = current(activityId);
        if (!StringUtils.hasText(currentToken)) {
            return false;
        }
        // 定长比较，避免长度差导致的早退泄露信息
        return java.security.MessageDigest.isEqual(
                currentToken.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                token.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /** 有效期（秒），供接口返回过期时间。 */
    public long ttlSeconds() {
        return ttlSeconds;
    }

    private String randomToken() {
        byte[] buf = new byte[24];
        RANDOM.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }
}
