package sicau.policialPartyManager.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import sicau.policialPartyManager.api.dto.Result;
import sicau.policialPartyManager.config.CurrentUser;
import sicau.policialPartyManager.model.records.User;
import sicau.policialPartyManager.api.dto.CurrentUserResponse;
import sicau.policialPartyManager.api.dto.LoginRequest;
import sicau.policialPartyManager.api.dto.LoginResponse;
import sicau.policialPartyManager.service.AuthService;

@Tag(name = "认证", description = "登录、获取当前用户信息")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户登录", description = "使用用户名和密码登录，返回 JWT token")
    @PostMapping("/login/userpass")
    public Result<LoginResponse> loginUsernamePassword(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.loginUsernamePassword(request));
    }

    @Operation(summary = "用户登录", description = "使用邮箱和验证码登录，返回 JWT token")
    @PostMapping("/login/email")
    public Result<LoginResponse> loginEmail(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.loginEmail(request));
    }

    @Operation(summary = "用户登录", description = "使用手机号和验证码登录，返回 JWT token")
    @PostMapping("/login/phone")
    public Result<LoginResponse> loginPhone(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.loginPhone(request));
    }

    @Operation(summary = "获取验证码", description = "获取邮箱或手机号的验证码")
    @PostMapping("/verifyCode")
    public Result<?> verifyCode(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.verifyCode(request));
    }

    @Operation(summary = "退出登录", description = "退出当前登录用户")
    @PostMapping("/logout")
    public Result<LoginResponse> logout(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.logout(request));
    }

    @Operation(summary = "刷新 JWT token", description = "使用刷新令牌刷新 JWT token")
    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.refresh(request));
    }
}
