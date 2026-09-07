package sicau.policialPartyManager.log;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import sicau.policialPartyManager.model.records.User;

/**
 * 从 Spring Security 上下文解析当前登录者（拦截器与异常处理器共用）。
 */
public final class AuthContext {

    private AuthContext() {
    }

    /** 当前登录用户信息；未登录返回 null */
    public static CurrentUser current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Long userId = null;
        if (authentication.getPrincipal() instanceof Long id) {
            userId = id;
        }
        if (authentication.getDetails() instanceof User user) {
            return new CurrentUser(userId, user.username(), user.role());
        }
        return userId == null ? null : new CurrentUser(userId, null, null);
    }

    /** 当前登录者摘要 */
    public record CurrentUser(Long userId, String username, String role) {
    }
}
