package sicau.policialPartyManager.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sicau.policialPartyManager.api.dto.IdentityChangeVo;
import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.api.dto.Result;
import sicau.policialPartyManager.config.CurrentUser;
import sicau.policialPartyManager.model.records.User;
import sicau.policialPartyManager.service.IdentityChangeService;

/**
 * 成员侧：我的身份变更历史（#44）。只读，发起/审批在管理面。
 */
@Tag(name = "个人中心-身份变更", description = "我的身份变更单历史（只读）")
@RestController
@RequestMapping("/api/v4/user/identity-changes")
@RequiredArgsConstructor
public class UserIdentityChangeController {

    private final IdentityChangeService identityChangeService;

    @Operation(summary = "我的变更单分页", description = "仅本人数据；含状态与审批意见")
    @GetMapping("/mine/page")
    public Result<PageResult<IdentityChangeVo>> mine(@RequestParam(defaultValue = "1") long page,
                                                     @RequestParam(defaultValue = "10") long size,
                                                     @CurrentUser User user) {
        return Result.ok(identityChangeService.pageMine(page, size, user));
    }
}
