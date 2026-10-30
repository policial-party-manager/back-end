package sicau.policialPartyManager.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.api.dto.Result;
import sicau.policialPartyManager.api.dto.ThoughtReportSubmitRequest;
import sicau.policialPartyManager.api.dto.ThoughtReportVo;
import sicau.policialPartyManager.config.CurrentUser;
import sicau.policialPartyManager.model.records.User;
import sicau.policialPartyManager.service.ThoughtReportService;

/**
 * 思想汇报（成员侧）：提交、查看自己的、撤回未审阅的。
 */
@Tag(name = "思想汇报", description = "成员提交思想汇报（审阅人自动派定为本支部书记）")
@RestController
@RequestMapping("/api/v4/thought-reports")
@RequiredArgsConstructor
public class ThoughtReportController {

    private final ThoughtReportService thoughtReportService;

    @Operation(summary = "提交思想汇报",
            description = "审阅人由服务端按所属支部秘书自动派定；附件先经 POST /api/v4/uploads 上传后以 file: 路径传入")
    @PostMapping
    public Result<ThoughtReportVo> submit(@Valid @RequestBody ThoughtReportSubmitRequest request,
                                          @CurrentUser User user) {
        return Result.ok(thoughtReportService.submit(request, user));
    }

    @Operation(summary = "我的汇报分页", description = "仅本人数据；可按状态过滤（1 待审阅 / 2 通过 / 3 驳回）；列表不含正文")
    @GetMapping("/mine/page")
    public Result<PageResult<ThoughtReportVo>> mine(@RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "10") long size,
                                                    @RequestParam(required = false) Integer status,
                                                    @CurrentUser User user) {
        return Result.ok(thoughtReportService.pageMine(page, size, status, user));
    }

    @Operation(summary = "撤回汇报", description = "仅未审阅（status=1）可撤回；已审阅返回 400")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, @CurrentUser User user) {
        thoughtReportService.deleteMine(id, user);
        return Result.ok();
    }
}
