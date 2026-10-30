package sicau.policialPartyManager.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.api.dto.Result;
import sicau.policialPartyManager.api.dto.ThoughtReportVo;
import sicau.policialPartyManager.service.ThoughtReportService;

/**
 * 思想汇报（超管只读兜底）：秘书缺岗时全局查看。与 AdminController 共享前缀。
 */
@Tag(name = "管理员后台-思想汇报", description = "全局只读查看思想汇报")
@RestController
@RequestMapping("/api/v4/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminThoughtReportController {

    private final ThoughtReportService thoughtReportService;

    @Operation(summary = "思想汇报全局分页", description = "可按提交人/状态过滤；只读")
    @GetMapping("/thought-reports/page")
    public Result<PageResult<ThoughtReportVo>> page(@RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "10") long size,
                                                    @RequestParam(required = false) Integer status,
                                                    @RequestParam(required = false) Long userId) {
        return Result.ok(thoughtReportService.pageAll(page, size, status, userId));
    }
}
