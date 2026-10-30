package sicau.policialPartyManager.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sicau.policialPartyManager.api.dto.Result;
import sicau.policialPartyManager.api.dto.UserProfileUpdateRequest;
import sicau.policialPartyManager.api.dto.UserProfileVo;
import sicau.policialPartyManager.config.CurrentUser;
import sicau.policialPartyManager.model.records.User;
import sicau.policialPartyManager.service.UserService;

/**
 * 个人中心：当前用户资料查看与编辑（出入参均类型化，见 #36）。
 */
@Tag(name = "个人中心", description = "当前用户个人信息查看与编辑")
@RestController
@RequestMapping("/api/v4/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "获取个人信息", description = "账号 + 成员档案 + 所属支部")
    @GetMapping("/profile")
    public Result<UserProfileVo> profile(@CurrentUser User user) {
        return Result.ok(userService.profile(user));
    }

    @Operation(summary = "编辑个人信息",
            description = "部分更新：未传或空白字段保持原值；未知字段忽略；"
                    + "支部归属不可自助修改（由管理员维护）")
    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody UserProfileUpdateRequest request,
                                      @CurrentUser User user) {
        userService.updateProfile(request, user);
        return Result.ok();
    }
}
