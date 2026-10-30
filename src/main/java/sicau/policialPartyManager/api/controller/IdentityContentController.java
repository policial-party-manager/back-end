package sicau.policialPartyManager.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.api.dto.Result;
import sicau.policialPartyManager.model.entity.Identity;
import sicau.policialPartyManager.service.IdentityService;

/**
 * 政治身份字典只读（#44）：登录即可（发起变更时选择目标身份）。
 */
@Tag(name = "内容-身份字典", description = "政治身份字典列表（登录可读）")
@RestController
@RequestMapping("/api/v2/content/identities")
@RequiredArgsConstructor
public class IdentityContentController {

    private final IdentityService identityService;

    @Operation(summary = "身份字典列表", description = "按 level 升序；role_id 仅展示映射，不参与鉴权")
    @GetMapping("/page")
    public Result<PageResult<Identity>> page(@RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "10") long size) {
        return Result.ok(identityService.page(page, size));
    }
}
