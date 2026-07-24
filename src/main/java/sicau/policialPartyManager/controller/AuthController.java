package sicau.policialPartyManager.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import sicau.policialPartyManager.common.Result;
import sicau.policialPartyManager.config.CurrentUser;
import sicau.policialPartyManager.config.JwtAuthFilter.TokenUser;
import sicau.policialPartyManager.dto.LoginRequest;
import sicau.policialPartyManager.dto.LoginResponse;
import sicau.policialPartyManager.service.AuthService;

import java.util.Map;

@Tag(name = "认证", description = "登录、获取当前用户信息")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户登录", description = "使用用户名和密码登录，返回 JWT token")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @Operation(summary = "获取当前用户", description = "返回当前登录用户的基本信息")
    @GetMapping("/current")
    public Result<?> current(@CurrentUser TokenUser user) {
        return Result.ok(Map.of(
                "userId", user.userId(),
                "username", user.username(),
                "role", user.role()
        ));
    }
}
