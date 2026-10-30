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
import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.api.dto.Result;
import sicau.policialPartyManager.api.dto.ThoughtReportVo;
import sicau.policialPartyManager.api.dto.ThoughtReviewRequest;
import sicau.policialPartyManager.config.CurrentUser;
import sicau.policialPartyManager.model.records.User;
import sicau.policialPartyManager.service.ThoughtReportService;

/**
 * 思想汇报（审阅侧）：支部书记查看与审阅指派给自己的汇报。
 * 越权访问统一「不存在」语义（400），不泄露记录存在性。
 */
@Tag(name = "思想汇报审阅", description = "branch_admin 审阅指派给自己的思想汇报")
@RestController
@RequestMapping("/api/v4/review/thought-reports")
@PreAuthorize("hasRole('BRANCH_ADMIN')")
@RequiredArgsConstructor
public class ThoughtReportReviewController {

    private final ThoughtReportService thoughtReportService;

    @Operation(summary = "待我审阅分页", description = "仅指派给当前用户的汇报；可按状态过滤；列表不含正文")
    @GetMapping("/page")
    public Result<PageResult<ThoughtReportVo>> page(@RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "10") long size,
                                                    @RequestParam(required = false) Integer status,
                                                    @CurrentUser User reviewer) {
        return Result.ok(thoughtReportService.pageForReviewer(page, size, status, reviewer));
    }

    @Operation(summary = "汇报详情", description = "含正文；非指派者访问返回「思想汇报不存在」")
    @GetMapping("/{id}")
    public Result<ThoughtReportVo> get(@PathVariable Long id, @CurrentUser User reviewer) {
        return Result.ok(thoughtReportService.getForReviewer(id, reviewer));
    }

    @Operation(summary = "审阅（通过/驳回）",
            description = "仅 status=1 可审；通过后重复审阅 400。驳回后成员重提将生成新记录，本条留痕")
    @PostMapping("/{id}/review")
    public Result<ThoughtReportVo> review(@PathVariable Long id,
                                          @Valid @RequestBody ThoughtReviewRequest request,
                                          @CurrentUser User reviewer) {
        return Result.ok(thoughtReportService.review(id, request, reviewer));
    }
}
