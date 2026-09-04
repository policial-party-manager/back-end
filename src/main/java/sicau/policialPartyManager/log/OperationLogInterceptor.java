package sicau.policialPartyManager.log;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 统一操作日志拦截器：
 * <ul>
 *   <li>preHandle：填充线程级请求上下文（IP/UA），记录开始时间</li>
 *   <li>afterCompletion：对 /api/** 下的非 GET 写操作自动发布 OPERATION 日志（带操作者/耗时/结果）</li>
 * </ul>
 * 登录/登出、错误由显式事件记录；GET 查询、文档、日志查询接口不重复记录。
 */
@Component
@RequiredArgsConstructor
public class OperationLogInterceptor implements HandlerInterceptor {

    private static final String START_ATTR = OperationLogInterceptor.class.getName() + ".start";

    private final OperationLogPublisher publisher;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        RequestContext.set(request);
        request.setAttribute(START_ATTR, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                @NonNull Object handler, Exception ex) {
        try {
            if (shouldRecord(request)) {
                record(request, response, ex);
            }
        } finally {
            RequestContext.clear();
        }
    }

    private boolean shouldRecord(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        if ("GET".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method)) {
            return false;
        }
        if (!uri.startsWith("/api/") || uri.contains("/api/v1/auth/") || uri.contains("/admin/logs")) {
            return false;
        }
        return true;
    }

    private void record(HttpServletRequest request, HttpServletResponse response, Exception ex) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String module = moduleFor(uri);
        String action = actionFor(uri, method);

        long start = request.getAttribute(START_ATTR) instanceof Long s ? s : 0L;
        long duration = start > 0 ? System.currentTimeMillis() - start : 0L;
        boolean success = ex == null && response.getStatus() < 400;

        String message;
        if (ex != null) {
            message = "请求处理异常：" + ex.getClass().getSimpleName() + " - "
                    + (ex.getMessage() == null ? "" : ex.getMessage());
        } else {
            message = module + "-" + action;
        }
        publisher.operation(module, action, method, uri, success, response.getStatus(), duration, message);
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
        if (uri.contains("/api/v4/admin/activities") || uri.contains("/activity-types")) {
            return "活动管理";
        }
        if (uri.contains("/api/v4/admin/news")) {
            return "新闻管理";
        }
        if (uri.contains("/api/v4/admin/notices")) {
            return "公告管理";
        }
        if (uri.contains("/api/v4/user")) {
            return "个人中心";
        }
        if (uri.contains("/api/v4/admin")) {
            return "管理员";
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
