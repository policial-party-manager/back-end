package sicau.policialPartyManager.log;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 操作日志发布器：统一补全（操作者/IP/UA/时间/id）后发布 {@link OperationLogEvent}，
 * 调用方只关心要记录什么，不关心如何落库。
 */
@Component
@RequiredArgsConstructor
public class OperationLogPublisher {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME
            .withZone(ZoneId.systemDefault());

    private final ApplicationEventPublisher eventPublisher;

    /** 发布一条完整日志（缺省字段自动补全） */
    public void publish(OperationLog log) {
        if (log == null) {
            return;
        }
        if (!StringUtils.hasText(log.getId())) {
            log.setId(UUID.randomUUID().toString().replace("-", ""));
        }
        if (log.getOccurTime() == null) {
            log.setOccurTime(System.currentTimeMillis());
        }
        if (!StringUtils.hasText(log.getOccurTimeText())) {
            log.setOccurTimeText(ISO.format(Instant.ofEpochMilli(log.getOccurTime())));
        }
        // 合并当前请求上下文（IP/UA）
        RequestContext.HttpRequestInfo ctx = RequestContext.get();
        if (ctx != null) {
            if (!StringUtils.hasText(log.getIpAddress())) {
                log.setIpAddress(ctx.ip());
            }
            if (!StringUtils.hasText(log.getUserAgent())) {
                log.setUserAgent(ctx.userAgent());
            }
        }
        // 合并当前登录者（登录前无 SecurityContext，由调用方显式设置 operatorName）
        AuthContext.CurrentUser current = AuthContext.current();
        if (current != null) {
            if (log.getOperatorId() == null) {
                log.setOperatorId(current.userId());
            }
            if (!StringUtils.hasText(log.getOperatorName())) {
                log.setOperatorName(current.username());
            }
            if (!StringUtils.hasText(log.getRole())) {
                log.setRole(current.role());
            }
        }
        eventPublisher.publishEvent(new OperationLogEvent(this, log));
    }

    /** 登录/登出事件 */
    public void auth(String logType, String account, String channel, boolean success, String reason) {
        OperationLog log = base(logType, "认证", logType);
        log.setAction(switch (logType) {
            case LogType.LOGIN -> success ? "登录成功" : "登录失败";
            case LogType.LOGOUT -> success ? "退出登录" : "退出登录失败";
            default -> logType;
        });
        log.setOperatorName(account);
        log.setMessage(StringUtils.hasText(reason) ? reason : channel);
        log.setSuccess(success);
        publish(log);
    }

    /** 业务错误（ERROR） */
    public void error(String module, String errorType, String message, int resultCode) {
        OperationLog log = base(LogType.ERROR, module, "错误");
        log.setErrorType(errorType);
        log.setMessage(message);
        log.setSuccess(false);
        log.setResultCode(resultCode);
        publish(log);
    }

    /** 写操作日志（拦截器） */
    public void operation(String module, String action, String method, String path, boolean success,
                          int resultCode, long durationMs, String message) {
        OperationLog log = base(LogType.OPERATION, module, action);
        log.setMethod(method);
        log.setPath(path);
        log.setSuccess(success);
        log.setResultCode(resultCode);
        log.setDurationMs(durationMs);
        log.setMessage(message);
        publish(log);
    }

    private OperationLog base(String logType, String module, String action) {
        OperationLog log = new OperationLog();
        log.setLogType(logType);
        log.setModule(module);
        log.setAction(action);
        return log;
    }
}
