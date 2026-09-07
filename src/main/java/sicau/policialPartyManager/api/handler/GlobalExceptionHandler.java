package sicau.policialPartyManager.api.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import sicau.policialPartyManager.api.dto.Result;
import sicau.policialPartyManager.log.OperationLogPublisher;

/**
 * 全局异常处理：
 * <ul>
 *   <li>参数/表单校验异常（@NotBlank/@Pattern/@Valid 等）统一返回 400 与首个校验错误信息</li>
 *   <li>业务异常 IllegalArgumentException 返回 400；基础设施异常 IllegalStateException 返回 500 与原信息</li>
 *   <li>错误统一发布 ERROR 日志事件（登录相关路径由登录事件记录，避免重复；日志查询接口自身不记录）</li>
 *   <li>其余异常记录日志并返回 500</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final OperationLogPublisher logPublisher;

    /** @RequestBody + @Valid 校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElseGet(() -> e.getBindingResult().getGlobalErrors().stream()
                        .findFirst()
                        .map(DefaultMessageSourceResolvable::getDefaultMessage)
                        .orElse("参数校验失败"));
        publishError(request, e, HttpStatus.BAD_REQUEST.value(), message);
        return Result.fail(message);
    }

    /** 表单绑定（@ModelAttribute / 表单对象）校验失败 */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleBindException(BindException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("参数校验失败");
        publishError(request, e, HttpStatus.BAD_REQUEST.value(), message);
        return Result.fail(message);
    }

    /** 方法参数级校验失败（@Validated 分组 / 路径与查询参数上的约束） */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleConstraintViolation(ConstraintViolationException e, HttpServletRequest request) {
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(ConstraintViolation::getMessage)
                .orElse("参数校验失败");
        publishError(request, e, HttpStatus.BAD_REQUEST.value(), message);
        return Result.fail(message);
    }

    /** 请求体缺失或 JSON 无法解析 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMessageNotReadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        String message = "请求体缺失或格式不正确";
        publishError(request, e, HttpStatus.BAD_REQUEST.value(), message);
        return Result.fail(message);
    }

    /** 缺少必填请求参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMissingParameter(MissingServletRequestParameterException e, HttpServletRequest request) {
        String message = "缺少必填参数：" + e.getParameterName();
        publishError(request, e, HttpStatus.BAD_REQUEST.value(), message);
        return Result.fail(message);
    }

    /** 请求参数类型不匹配 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String message = "参数类型不正确：" + e.getName();
        publishError(request, e, HttpStatus.BAD_REQUEST.value(), message);
        return Result.fail(message);
    }

    /** 请求方法不支持 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<?> handleMethodNotSupported(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        String message = "请求方法不支持：" + e.getMethod();
        publishError(request, e, HttpStatus.METHOD_NOT_ALLOWED.value(), message);
        return Result.fail(405, message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleIllegalArgument(IllegalArgumentException e, HttpServletRequest request) {
        publishError(request, e, HttpStatus.BAD_REQUEST.value(), e.getMessage());
        return Result.fail(e.getMessage());
    }

    /** 基础设施不可用（如 Elasticsearch 日志服务）等，返回 500 与原信息便于排查 */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleIllegalState(IllegalStateException e, HttpServletRequest request) {
        log.warn("基础设施异常", e);
        publishError(request, e, HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
        return Result.fail(500, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleException(Exception e, HttpServletRequest request) {
        log.error("服务器内部错误", e);
        publishError(request, e, HttpStatus.INTERNAL_SERVER_ERROR.value(), "服务器内部错误");
        return Result.fail(500, "服务器内部错误");
    }

    /** 错误记录发布（登录路径由登录事件记录、日志查询接口不自我记录） */
    private void publishError(HttpServletRequest request, Throwable e, int status, String message) {
        try {
            String uri = request == null ? "" : (request.getRequestURI() == null ? "" : request.getRequestURI());
            if (uri.contains("/api/v1/auth/") || uri.contains("/admin/logs")
                    || uri.contains("doc.html") || uri.contains("api-docs") || uri.contains("webjars")) {
                return;
            }
            String detail = message == null || message.isBlank() ? e.getMessage() : message;
            if (detail == null || detail.isBlank()) {
                detail = e.getClass().getSimpleName();
            }
            logPublisher.error("系统", e.getClass().getSimpleName(), detail, status);
        } catch (Exception ex) {
            log.debug("发布错误日志事件失败", ex);
        }
    }
}
