package sicau.policialPartyManager.config;

import java.lang.annotation.*;

/**
 * 在 Controller 方法参数上注入当前登录用户，替代手动从 Authentication 中取
 *
 * <pre>{@code
 * @GetMapping("/profile")
 * public Result<?> profile(@CurrentUser TokenUser user) { ... }
 * }</pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {
}
