package sicau.policialPartyManager.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.api.dto.Result;
import sicau.policialPartyManager.model.entity.ActivitySignup;
import sicau.policialPartyManager.service.ActivitySignupService;

/**
 * 管理端：活动报名记录查询。与 AdminController 共享 /api/v4/admin 前缀。
 */
@Tag(name = "管理员后台-报名", description = "活动报名记录查询")
@RestController
@RequestMapping("/api/v4/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminActivitySignupController {

    private final ActivitySignupService activitySignupService;

    @Operation(summary = "活动报名记录分页", description = "按活动查报名记录，可按报名人过滤")
    @GetMapping("/activities/{activityId}/signups/page")
    public Result<PageResult<ActivitySignup>> pageSignups(@PathVariable Long activityId,
                                                          @RequestParam(defaultValue = "1") long page,
                                                          @RequestParam(defaultValue = "10") long size,
                                                          @RequestParam(required = false) Long userId) {
        return Result.ok(activitySignupService.pageByActivity(page, size, activityId, userId));
    }
}
