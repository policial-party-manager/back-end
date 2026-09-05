package sicau.policialPartyManager.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
import sicau.policialPartyManager.service.SsoService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Tag(name = "认证", description = "登录、验证码、退出登录、刷新令牌、学校 CAS 统一身份认证")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SsoService ssoService;
    private final OperationLogPublisher logPublisher;

    @Value("${frontend.base-url:}")
    private String frontendBaseUrl;

    @Value("${frontend.sso-landing-path:/sso/login}")
    private String ssoLandingPath;

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

    // ======================= 学校 CAS 统一身份认证 =======================

    @Operation(summary = "CAS 登录跳转", description = "返回学校统一身份认证登录页地址（前端 location 跳转即可）")
    @GetMapping("/sso/login")
    public Result<Map<String, String>> ssoLogin(HttpServletRequest request, HttpServletResponse response) {
        String url = ssoService.buildCasLoginUrl(request, response);
        if (url == null) {
            throw new IllegalArgumentException("CAS 登录地址生成失败，请稍后重试");
        }
        return Result.ok(Map.of("url", url));
    }

    @Operation(summary = "CAS 回调", description = "CAS 验证后回跳：验票 → 仅已有账号 → 签发本站 token → 302 回前端落地页")
    @GetMapping("/sso/callback")
    public void ssoCallback(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String accountHint = firstText(request.getParameter("userId"), "CAS 用户");
        try {
            LoginResponse login = ssoService.handleCallback(request);
            logPublisher.auth(LogType.LOGIN, login.getUsername(), "CAS统一认证", true, "登录成功");
            redirectToFrontend(response, login.getAccessToken(), login.getRefreshToken(), null);
        } catch (IllegalArgumentException e) {
            logPublisher.auth(LogType.LOGIN, accountHint, "CAS统一认证", false, e.getMessage());
            redirectToFrontend(response, null, null, e.getMessage());
        }
    }

    /** 成功：landing?access_token=&refresh_token=；失败：landing?error=原因；未配置前端地址时返回文本提示 */
    private void redirectToFrontend(HttpServletResponse response, String accessToken, String refreshToken,
                                    String error) throws IOException {
        if (!StringUtils.hasText(frontendBaseUrl)) {
            response.setContentType("text/plain;charset=UTF-8");
            if (StringUtils.hasText(error)) {
                response.getWriter().write("SSO 登录失败：" + error);
            } else {
                response.getWriter().write("SSO 登录成功（未配置 frontend.base-url，无法回跳）");
            }
            return;
        }
        String landing = StringUtils.hasText(ssoLandingPath) && ssoLandingPath.startsWith("/")
                ? ssoLandingPath : "/" + (ssoLandingPath == null ? "sso/login" : ssoLandingPath);
        StringBuilder target = new StringBuilder(frontendBaseUrl).append(landing).append('?');
        if (StringUtils.hasText(error)) {
            target.append("error=").append(URLEncoder.encode(error, StandardCharsets.UTF_8));
        } else {
            target.append("access_token=").append(accessToken)
                    .append("&refresh_token=").append(refreshToken);
        }
        response.sendRedirect(target.toString());
    }

    private String firstText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String currentUsernameOrUnknown() {
        AuthContext.CurrentUser current = AuthContext.current();
        return current != null && StringUtils.hasText(current.username()) ? current.username() : "未知账号";
    }
}
