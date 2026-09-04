package sicau.policialPartyManager.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import sicau.policialPartyManager.api.dto.Result;
import sicau.policialPartyManager.config.CurrentUser;
import sicau.policialPartyManager.model.records.User;
import sicau.policialPartyManager.service.UserService;

import java.util.Map;

/**
 * todo 将Object改为dto
 */
@Tag(name = "个人中心", description = "当前用户个人信息查看与编辑")
@RestController
@RequestMapping("/api/v4/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "获取个人信息")
    @GetMapping("/profile")
    public Result<?> profile(@CurrentUser User user) {
        return Result.ok(userService.profile(user));
    }

    /**
     * todo 将map修改为dto
     */
    @Operation(summary = "编辑个人信息")
    @PutMapping("/profile")
    public Result<?> updateProfile(@RequestBody Map<String, Object> body, @CurrentUser User user) {
        userService.updateProfile(body,user);
        return Result.ok();
    }
}
