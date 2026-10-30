package sicau.policialPartyManager.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sicau.policialPartyManager.api.dto.IdentityChangeApplyRequest;
import sicau.policialPartyManager.api.dto.IdentityChangeVo;
import sicau.policialPartyManager.api.dto.IdentityReviewRequest;
import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.api.dto.Result;
import sicau.policialPartyManager.config.CurrentUser;
import sicau.policialPartyManager.model.records.User;
import sicau.policialPartyManager.service.IdentityChangeService;

/**
 * 身份变更单（#44，两阶段）：支管（限本支部）或超管发起，超管审批。
 * 审批权限注解声明 + 服务内角色检查双保险；越权读单统一「变更单不存在」。
 * 成员侧查询在 UserIdentityChangeController（/api/v4/user/identity-changes）。
 */
@Tag(name = "身份变更", description = "政治身份变更单的发起与审批（支管限本支部，审批仅超管）")
@RestController
@RequestMapping("/api/v4/identity-changes")
@RequiredArgsConstructor
public class IdentityChangeController {

    private final IdentityChangeService identityChangeService;

    @Operation(summary = "发起身份变更",
            description = "branch_admin 限本支部成员、super_admin 不限；from 取目标当前身份快照（首次登记为空）；"
                    + "同一用户仅允许一张待审批单；生效不会联动登录角色")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','BRANCH_ADMIN')")
    @PostMapping
    public Result<IdentityChangeVo> apply(@Valid @RequestBody IdentityChangeApplyRequest request,
                                          @CurrentUser User operator) {
        return Result.ok(identityChangeService.apply(request, operator));
    }

    @Operation(summary = "审批身份变更",
            description = "仅 super_admin；通过=写回当前身份，驳回=现身份不动；仅待审批单可审")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/{id}/review")
    public Result<IdentityChangeVo> review(@PathVariable Long id,
                                           @Valid @RequestBody IdentityReviewRequest request,
                                           @CurrentUser User reviewer) {
        return Result.ok(identityChangeService.approve(id, request.getPass(), request.getRemark(), reviewer));
    }

    @Operation(summary = "我可管理的变更单分页",
            description = "超管=全局（可按 userId/status 过滤）；支管=本支部成员的单")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','BRANCH_ADMIN')")
    @GetMapping("/managed/page")
    public Result<PageResult<IdentityChangeVo>> managed(@RequestParam(defaultValue = "1") long page,
                                                        @RequestParam(defaultValue = "10") long size,
                                                        @RequestParam(required = false) Long userId,
                                                        @RequestParam(required = false) Integer status,
                                                        @CurrentUser User operator) {
        if ("super_admin".equalsIgnoreCase(operator.role())) {
            return Result.ok(identityChangeService.pageForAdmin(page, size, userId, status));
        }
        return Result.ok(identityChangeService.pageForBranch(page, size, operator));
    }

    @Operation(summary = "变更单详情", description = "可管理范围内可见；越权统一「变更单不存在」")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','BRANCH_ADMIN')")
    @GetMapping("/{id}")
    public Result<IdentityChangeVo> get(@PathVariable Long id, @CurrentUser User operator) {
        return Result.ok(identityChangeService.detail(id, operator));
    }
}
