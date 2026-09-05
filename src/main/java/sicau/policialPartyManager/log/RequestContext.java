package sicau.policialPartyManager.log;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * 线程级请求上下文：由 {@link OperationLogFilter} 在请求进入时填充客户端 IP / UA，
 * 供登录等无请求对象的业务点组装日志，请求结束后清理。
 */
public final class RequestContext {

    private static final ThreadLocal<HttpRequestInfo> HOLDER = new ThreadLocal<>();

    private RequestContext() {
    }

    public static void set(HttpServletRequest request) {
        HOLDER.set(new HttpRequestInfo(resolveIp(request), request.getHeader("User-Agent")));
    }

    public static HttpRequestInfo get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    private static String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        String real = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(real)) {
            return real.trim();
        }
        return request.getRemoteAddr();
    }

    /** 请求基本信息 */
    public record HttpRequestInfo(String ip, String userAgent) {
    }
}
