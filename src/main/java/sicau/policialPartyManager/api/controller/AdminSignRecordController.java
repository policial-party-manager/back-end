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
import sicau.policialPartyManager.model.entity.SignRecord;
import sicau.policialPartyManager.service.SignRecordService;

/**
 * 管理端：活动签到记录查询。
 * 与 AdminController 共享 /api/v4/admin 前缀，但按模块独立成类（避免巨型控制器继续膨胀）。
 */
@Tag(name = "管理员后台-签到", description = "活动签到记录查询")
@RestController
@RequestMapping("/api/v4/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminSignRecordController {

    private final SignRecordService signRecordService;

    @Operation(summary = "活动签到记录分页", description = "按活动查签到记录，可按签到人过滤")
    @GetMapping("/activities/{activityId}/signs/page")
    public Result<PageResult<SignRecord>> pageSigns(@PathVariable Long activityId,
                                                    @RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "10") long size,
                                                    @RequestParam(required = false) Long userId) {
        return Result.ok(signRecordService.pageByActivity(page, size, activityId, userId));
    }
}
