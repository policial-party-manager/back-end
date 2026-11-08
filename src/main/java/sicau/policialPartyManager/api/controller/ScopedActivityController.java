package sicau.policialPartyManager.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.api.dto.Result;
import sicau.policialPartyManager.config.CurrentUser;
import sicau.policialPartyManager.model.entity.ActivitySignup;
import sicau.policialPartyManager.model.entity.SignRecord;
import sicau.policialPartyManager.model.records.User;
import sicau.policialPartyManager.service.ActivitySignupService;
import sicau.policialPartyManager.service.Impl.ActivityScopeGuard;
import sicau.policialPartyManager.service.SignRecordService;

/**
 * 支委可用的活动域只读端点（#71）。
 * <p>超管全院、支委限本支部；跨支部读取统一「活动不存在」。
 * <p>删除活动、查看全院活动仍属超管专属，不在本控制器内。
 */
@Tag(name = "活动域（支委）", description = "本支部活动的报名与签到查询")
@RestController
@RequestMapping("/api/v4/activities")
@RequiredArgsConstructor
public class ScopedActivityController {

    private final ActivityScopeGuard activityScopeGuard;
    private final ActivitySignupService activitySignupService;
    private final SignRecordService signRecordService;

    @Operation(summary = "活动报名记录分页（按操作者范围）")
    @GetMapping("/{activityId}/signups/page")
    public Result<PageResult<ActivitySignup>> signups(@CurrentUser User operator,
                                                      @PathVariable Long activityId,
                                                      @RequestParam(defaultValue = "1") long page,
                                                      @RequestParam(defaultValue = "10") long size,
                                                      @RequestParam(required = false) Long userId) {
        activityScopeGuard.requireManageable(operator, activityId);
        return Result.ok(activitySignupService.pageByActivity(page, size, activityId, userId));
    }

    @Operation(summary = "活动签到记录分页（按操作者范围）")
    @GetMapping("/{activityId}/signs/page")
    public Result<PageResult<SignRecord>> signs(@CurrentUser User operator,
                                                @PathVariable Long activityId,
                                                @RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) Long userId) {
        activityScopeGuard.requireManageable(operator, activityId);
        return Result.ok(signRecordService.pageByActivity(page, size, activityId, userId));
    }
}
