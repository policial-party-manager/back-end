package sicau.policialPartyManager.log;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 统一操作日志过滤器（注册在 Spring Security 链内、JwtAuthFilter 之后）：
 * <ul>
 *   <li>对所有 /api/** 请求填充线程级请求上下文（IP/UA），供登录等事件使用</li>
 *   <li>对非 GET 写操作自动记录 OPERATION 日志（可覆盖 401/403 与请求抛错场景）</li>
 * </ul>
 * 登录/登出由认证事件负责（含账号级明细），此处不重复记录；GET 查询与日志查询接口不记录。
 */
@RequiredArgsConstructor
public class OperationLogFilter extends OncePerRequestFilter {

    private final OperationLogPublisher publisher;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri == null || !uri.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        RequestContext.set(request);
        long start = System.currentTimeMillis();
        Throwable failure = null;
        try {
            filterChain.doFilter(request, response);
        } catch (Throwable t) {
            failure = t;
            throw t;
        } finally {
            try {
                if (shouldRecord(request)) {
                    record(request, response, failure, System.currentTimeMillis() - start);
                }
            } finally {
                RequestContext.clear();
            }
        }
    }

    private boolean shouldRecord(HttpServletRequest request) {
        String method = request.getMethod();
        if ("GET".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method)
                || "HEAD".equalsIgnoreCase(method)) {
            return false;
        }
        String uri = request.getRequestURI();
        if (uri.contains("/api/v1/auth/") || uri.contains("/admin/logs")) {
            return false;
        }
        return true;
    }

    private void record(HttpServletRequest request, HttpServletResponse response,
                        Throwable failure, long durationMs) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String module = moduleFor(uri);
        String action = actionFor(uri, method);

        boolean success = failure == null && response.getStatus() < 400;
        String message;
        if (failure != null) {
            message = "请求处理异常：" + failure.getClass().getSimpleName() + " - "
                    + (failure.getMessage() == null ? "" : failure.getMessage());
        } else {
            message = module + "-" + action;
        }
        publisher.operation(module, action, method, uri, success, response.getStatus(), durationMs, message);
    }

    /** 路径 → 业务模块 */
    private String moduleFor(String uri) {
        if (uri.contains("/api/v4/admin/users")) {
            return "用户管理";
        }
        if (uri.contains("/api/v4/admin/branches") || uri.contains("/api/v2/branch")) {
            return "支部管理";
        }
        if (uri.contains("/api/v4/admin/roles")) {
            return "权限管理";
        }
        if (uri.contains("/activities") || uri.contains("/activity-types")) {
            return "活动管理";
        }
        if (uri.contains("/news")) {
            return "新闻管理";
        }
        if (uri.contains("/notices")) {
            return "公告管理";
        }
        if (uri.contains("/api/v4/user")) {
            return "个人中心";
        }
        if (uri.contains("/api/v4/admin")) {
            return "管理员";
        }
        if (uri.contains("/api/v2/content")) {
            return "内容";
        }
        return "其他";
    }

    /** 路径 + 方法 → 动作描述 */
    private String actionFor(String uri, String method) {
        if (uri.contains("/import")) {
            return "批量导入";
        }
        if (uri.contains("/template")) {
            return "下载模板";
        }
        if (uri.contains("/status")) {
            return "状态变更";
        }
        if (uri.contains("/password")) {
            return "重置密码";
        }
        if (uri.contains("/permissions")) {
            return "分配权限";
        }
        if (uri.contains("/publish")) {
            return "发布";
        }
        return switch (method) {
            case "POST" -> "新增";
            case "PUT" -> "编辑";
            case "DELETE" -> "删除";
            default -> method;
        };
    }
}
