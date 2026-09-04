package sicau.policialPartyManager.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import sicau.policialPartyManager.api.dto.LoginRequest;
import sicau.policialPartyManager.api.dto.LoginResponse;
import sicau.policialPartyManager.api.dto.Result;
import sicau.policialPartyManager.api.dto.ValidationGroups;
import sicau.policialPartyManager.log.AuthContext;
import sicau.policialPartyManager.log.LogType;
import sicau.policialPartyManager.log.OperationLogPublisher;
import sicau.policialPartyManager.service.AuthService;

@Tag(name = "认证", description = "登录、验证码、退出登录、刷新令牌、获取当前用户信息")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OperationLogPublisher logPublisher;

    @Operation(summary = "用户名密码登录", description = "使用用户名和密码登录，返回 JWT token")
    @PostMapping("/login/userpass")
    public Result<LoginResponse> loginUsernamePassword(
            @Validated(ValidationGroups.UserPass.class) @RequestBody LoginRequest request) {
        String account = firstText(request.getUsername(), "未知账号");
        try {
            LoginResponse response = authService.loginUsernamePassword(request);
            logPublisher.auth(LogType.LOGIN, account, "用户名密码", true, "登录成功");
            return Result.ok(response);
        } catch (RuntimeException e) {
            logPublisher.auth(LogType.LOGIN, account, "用户名密码", false, e.getMessage());
            throw e;
        }
    }

    @Operation(summary = "邮箱验证码登录", description = "使用邮箱和验证码登录，返回 JWT token")
    @PostMapping("/login/email")
    public Result<LoginResponse> loginEmail(
            @Validated(ValidationGroups.EmailLogin.class) @RequestBody LoginRequest request) {
        String account = firstText(request.getEmail(), "未知账号");
        try {
            LoginResponse response = authService.loginEmail(request);
            logPublisher.auth(LogType.LOGIN, account, "邮箱验证码", true, "登录成功");
            return Result.ok(response);
        } catch (RuntimeException e) {
            logPublisher.auth(LogType.LOGIN, account, "邮箱验证码", false, e.getMessage());
            throw e;
        }
    }

    @Operation(summary = "手机号验证码登录", description = "使用手机号和验证码登录，返回 JWT token（短信通道暂未接入）")
    @PostMapping("/login/phone")
    public Result<LoginResponse> loginPhone(
            @Validated(ValidationGroups.PhoneLogin.class) @RequestBody LoginRequest request) {
        String account = firstText(request.getPhone(), "未知账号");
        try {
            LoginResponse response = authService.loginPhone(request);
            logPublisher.auth(LogType.LOGIN, account, "手机号验证码", true, "登录成功");
            return Result.ok(response);
        } catch (RuntimeException e) {
            logPublisher.auth(LogType.LOGIN, account, "手机号验证码", false, e.getMessage());
            throw e;
        }
    }

    @Operation(summary = "发送验证码", description = "向邮箱或手机号发送验证码（短信通道暂未接入，仅存储+日志）")
    @PostMapping("/verifyCode")
    public Result<String> verifyCode(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.verifyCode(request));
    }

    @Operation(summary = "退出登录", description = "吊销 refresh token，使刷新接口失效；access token 由前端丢弃、到期自然失效")
    @PostMapping("/logout")
    public Result<Void> logout(@Validated(ValidationGroups.Token.class) @RequestBody LoginRequest request) {
        String account = currentUsernameOrUnknown();
        try {
            authService.logout(request);
            logPublisher.auth(LogType.LOGOUT, account, "退出登录", true, null);
            return Result.ok();
        } catch (RuntimeException e) {
            logPublisher.auth(LogType.LOGOUT, account, "退出登录", false, e.getMessage());
            throw e;
        }
    }

    @Operation(summary = "刷新 JWT token", description = "使用 refresh token 换取新的 access token / refresh token（轮换机制）")
    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@Validated(ValidationGroups.Token.class) @RequestBody LoginRequest request) {
        LoginResponse response = authService.refresh(request);
        logPublisher.operation("认证", "刷新令牌", "POST", "/api/v1/auth/refresh", true,
                200, 0L, "刷新 JWT token 成功");
        return Result.ok(response);
    }

    private String firstText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String currentUsernameOrUnknown() {
        AuthContext.CurrentUser current = AuthContext.current();
        return current != null && StringUtils.hasText(current.username()) ? current.username() : "未知账号";
    }
}
