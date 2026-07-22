package sicau.policialPartyManager.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import sicau.policialPartyManager.common.Result;
import sicau.policialPartyManager.config.JwtAuthFilter.TokenUser;
import sicau.policialPartyManager.dto.LoginRequest;
import sicau.policialPartyManager.dto.LoginResponse;
import sicau.policialPartyManager.service.AuthService;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @GetMapping("/current")
    public Result<?> current(Authentication authentication) {
        TokenUser user = (TokenUser) authentication.getDetails();
        return Result.ok(Map.of(
                "userId", user.userId(),
                "username", user.username(),
                "role", user.role()
        ));
    }
}
