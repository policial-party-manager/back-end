package sicau.policialPartyManager.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sicau.policialPartyManager.api.dto.BatchResult;
import sicau.policialPartyManager.api.dto.Result;
import sicau.policialPartyManager.config.CurrentUser;
import sicau.policialPartyManager.model.records.User;
import sicau.policialPartyManager.service.Impl.ScopedMemberServiceImpl;

import java.util.List;

/**
 * 成员批量操作（#82）：批量停用/恢复、批量调整所属支部。
 * <p>单次上限 200 条，超出整体拒绝；逐条执行，返回部分成功明细。
 */
@Tag(name = "成员批量操作", description = "批量停用/恢复、批量调整所属支部")
@RestController
@RequestMapping("/api/v4/admin/users/batch")
@RequiredArgsConstructor
public class MemberBatchController {

    private final ScopedMemberServiceImpl scopedMemberService;

    @Operation(summary = "批量停用/恢复", description = "status：1 启用 / 2 停用；单次最多 200 条")
    @PostMapping("/status")
    public Result<BatchResult> batchStatus(@CurrentUser User operator,
                                           @Valid @RequestBody BatchStatusRequest request) {
        return Result.ok(scopedMemberService.batchUpdateStatus(operator, request.getIds(), request.getStatus()));
    }

    @Operation(summary = "批量调整所属支部", description = "仅超级管理员；单次最多 200 条")
    @PostMapping("/branch")
    public Result<BatchResult> batchBranch(@CurrentUser User operator,
                                           @Valid @RequestBody BatchBranchRequest request) {
        return Result.ok(scopedMemberService.batchUpdateBranch(operator, request.getIds(), request.getBranchId()));
    }

    @Data
    public static class BatchStatusRequest {
        @NotEmpty(message = "请至少选择一条记录")
        @Size(max = 200, message = "单次批量最多 200 条，请分批操作")
        private List<Long> ids;
        @NotNull(message = "状态不能为空")
        private Integer status;
    }

    @Data
    public static class BatchBranchRequest {
        @NotEmpty(message = "请至少选择一条记录")
        @Size(max = 200, message = "单次批量最多 200 条，请分批操作")
        private List<Long> ids;
        @NotNull(message = "支部不能为空")
        private Long branchId;
    }
}
