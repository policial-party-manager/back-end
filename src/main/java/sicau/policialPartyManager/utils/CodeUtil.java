package sicau.policialPartyManager.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;

/**
 * 验证码工具：负责验证码的生成与 Redis 存取/校验。
 * <p>
 * 与发送渠道解耦：发送邮件走 {@link MailUtil}；短信渠道尚未接入（留白），
 * 未来接入时只需在发送处增加短信实现，无需改动本类。
 */
@Component
@RequiredArgsConstructor
public class CodeUtil {

    /** Redis key 前缀 */
    private static final String CODE_KEY_PREFIX = "auth:code:";
    /** 验证码有效期 */
    private static final Duration CODE_TTL = Duration.ofMinutes(5);

    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;

    /**
     * 为目标（邮箱/手机号）生成 6 位数字验证码并存入 Redis。
     *
     * @return 明文验证码，由调用方负责发送
     */
    public String createAndStore(String target) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        redisTemplate.opsForValue().set(key(target), code, CODE_TTL);
        return code;
    }

    /**
     * 校验验证码：匹配则删除并返回 true（一次性），不匹配返回 false。
     */
    public boolean verify(String target, String code) {
        if (target == null || code == null) {
            return false;
        }
        String stored = redisTemplate.opsForValue().get(key(target));
        if (code.equals(stored)) {
            redisTemplate.delete(key(target));
            return true;
        }
        return false;
    }

    /**
     * 删除目标验证码（清理用）。
     */
    public void delete(String target) {
        if (target != null) {
            redisTemplate.delete(key(target));
        }
    }

    private String key(String target) {
        return CODE_KEY_PREFIX + target;
    }
}
