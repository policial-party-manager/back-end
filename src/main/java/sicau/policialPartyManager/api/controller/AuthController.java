package sicau.policialPartyManager.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import sicau.policialPartyManager.api.dto.Result;
import sicau.policialPartyManager.api.dto.LoginRequest;
import sicau.policialPartyManager.api.dto.LoginResponse;
import sicau.policialPartyManager.service.AuthService;

@Tag(name = "认证", description = "登录、验证码、退出登录、刷新令牌、获取当前用户信息")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户名密码登录", description = "使用用户名和密码登录，返回 JWT token")
    @PostMapping("/login/userpass")
    public Result<LoginResponse> loginUsernamePassword(@RequestBody LoginRequest request) {
        return Result.ok(authService.loginUsernamePassword(request));
    }

    @Operation(summary = "邮箱验证码登录", description = "使用邮箱和验证码登录，返回 JWT token")
    @PostMapping("/login/email")
    public Result<LoginResponse> loginEmail(@RequestBody LoginRequest request) {
        return Result.ok(authService.loginEmail(request));
    }

    @Operation(summary = "手机号验证码登录", description = "使用手机号和验证码登录，返回 JWT token（短信通道暂未接入）")
    @PostMapping("/login/phone")
    public Result<LoginResponse> loginPhone(@RequestBody LoginRequest request) {
        return Result.ok(authService.loginPhone(request));
    }

    @Operation(summary = "发送验证码", description = "向邮箱或手机号发送验证码（短信通道暂未接入，仅存储+日志）")
    @PostMapping("/verifyCode")
    public Result<String> verifyCode(@RequestBody LoginRequest request) {
        return Result.ok(authService.verifyCode(request));
    }

    @Operation(summary = "退出登录", description = "吊销 refresh token，使刷新接口失效；access token 由前端丢弃、到期自然失效")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestBody LoginRequest request) {
        authService.logout(request);
        return Result.ok();
    }

    @Operation(summary = "刷新 JWT token", description = "使用 refresh token 换取新的 access token / refresh token（轮换机制）")
    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@RequestBody LoginRequest request) {
        return Result.ok(authService.refresh(request));
    }
}
