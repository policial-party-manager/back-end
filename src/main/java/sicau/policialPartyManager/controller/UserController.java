package sicau.policialPartyManager.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import sicau.policialPartyManager.common.Result;
import sicau.policialPartyManager.config.CurrentUser;
import sicau.policialPartyManager.config.JwtAuthFilter.TokenUser;
import sicau.policialPartyManager.entity.User;
import sicau.policialPartyManager.service.UserService;

import java.util.Map;

/**
 * todo 将Object改为dto
 */
@Tag(name = "个人中心", description = "当前用户个人信息查看与编辑")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "获取个人信息")
    @GetMapping("/profile")
    public Result<?> profile(@CurrentUser TokenUser user) {
        return Result.ok(userService.profile(user));
    }

    /**
     * todo 将map修改为dto
     */
    @Operation(summary = "编辑个人信息")
    @PutMapping("/profile")
    public Result<?> updateProfile(@RequestBody Map<String, Object> body, @CurrentUser TokenUser user) {
        userService.updateProfile(body,user);
        return Result.ok();
    }
}
