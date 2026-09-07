package sicau.policialPartyManager.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.api.dto.Result;
import sicau.policialPartyManager.model.records.User;
import sicau.policialPartyManager.repository.OperationMapper;
import sicau.policialPartyManager.config.CurrentUser;

import java.time.LocalDateTime;

/**
 * 操作历史记录接口（仅管理员可访问）
 */
@Tag(name = "历史记录", description = "操作日志查询接口")
@RestController
@RequestMapping("/api/v4/history")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class HistoryController {

    private final OperationMapper operationMapper;

    @Operation(summary = "操作日志分页列表", description = "keyword 匹配操作内容；可按操作人ID过滤")
    @GetMapping("/operation/page")
    public Result<PageResult<sicau.policialPartyManager.model.entity.Operation>> pageOperation(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        LambdaQueryWrapper<sicau.policialPartyManager.model.entity.Operation> q = new LambdaQueryWrapper<>();
        q.like(keyword != null, sicau.policialPartyManager.model.entity.Operation::getOperator, keyword)
         .eq(operatorId != null, sicau.policialPartyManager.model.entity.Operation::getOperatorId, operatorId)
         .ge(startTime != null, sicau.policialPartyManager.model.entity.Operation::getTime, LocalDateTime.parse(startTime))
         .le(endTime != null, sicau.policialPartyManager.model.entity.Operation::getTime, LocalDateTime.parse(endTime))
         .orderByDesc(sicau.policialPartyManager.model.entity.Operation::getTime);

        Page<sicau.policialPartyManager.model.entity.Operation> pg = new Page<>(page, size);
        Page<sicau.policialPartyManager.model.entity.Operation> result = operationMapper.selectPage(pg, q);
        return Result.ok(PageResult.of(result.getRecords(), result.getTotal(), page, size));
    }

    @Operation(summary = "操作日志详情")
    @GetMapping("/operation/{id}")
    public Result<sicau.policialPartyManager.model.entity.Operation> getOperation(@PathVariable Long id) {
        return Result.ok(operationMapper.selectById(id));
    }

    @Operation(summary = "当前用户的操作记录")
    @GetMapping("/operation/my")
    public Result<PageResult<sicau.policialPartyManager.model.entity.Operation>> myOperation(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @CurrentUser User user) {
        LambdaQueryWrapper<sicau.policialPartyManager.model.entity.Operation> q = new LambdaQueryWrapper<>();
        q.eq(sicau.policialPartyManager.model.entity.Operation::getOperatorId, user.userId())
         .orderByDesc(sicau.policialPartyManager.model.entity.Operation::getTime);

        Page<sicau.policialPartyManager.model.entity.Operation> pg = new Page<>(page, size);
        Page<sicau.policialPartyManager.model.entity.Operation> result = operationMapper.selectPage(pg, q);
        return Result.ok(PageResult.of(result.getRecords(), result.getTotal(), page, size));
    }
}
