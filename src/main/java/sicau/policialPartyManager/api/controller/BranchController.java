package sicau.policialPartyManager.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sicau.policialPartyManager.api.dto.Result;
import sicau.policialPartyManager.model.entity.Branch;
import sicau.policialPartyManager.service.BranchService;

import java.util.List;

/**
 * 党支部管理（旧版 v2 接口）。
 * <p>
 * <b>已废弃</b>：能力被 {@code /api/v4/admin/branches/*} 完全覆盖且更强（分页、keyword 搜索、
 * 名称唯一与书记必填校验、下拉选项、Excel 模板与批量导入），本控制器是其严格弱化子集。
 * 另两处缺陷：类级鉴权只放 {@code branch_admin}，超级管理员反而被拒；{@code save()} 不写 status
 * （曾因 DDL 默认 0 让新建支部立刻变成"已删除"，见 #42）。
 * <p>
 * 前端盘点（{@code policial-party-manager/front-end} HEAD）无任何 {@code /api/v2/branch} 调用，
 * 支部操作全走 v4。按 #51 流程先标废弃观察一个迭代，确认无旁路调用后连同
 * {@code BranchService} / {@code BranchServiceImpl} 一并删除。
 *
 * @deprecated 请改用 {@code /api/v4/admin/branches/*}，无替代计划保留本入口
 */
@Deprecated
@Tag(name = "党支部管理（已废弃，请用 /api/v4/admin/branches/*）",
        description = "**已废弃**：能力被 /api/v4/admin/branches/* 完全覆盖，前端已无调用，观察一个迭代后移除")
@RestController
@RequestMapping("/api/v2/branch")
@RequiredArgsConstructor
@PreAuthorize("hasRole('BRANCH_ADMIN')")
public class BranchController {

    private final BranchService branchService;

    @Operation(summary = "支部列表", deprecated = true,
            description = "已废弃，请改用 GET /api/v4/admin/branches/page")
    @Deprecated
    @GetMapping("/list")
    public Result<List<Branch>> list() {
        return Result.ok(branchService.listAll());
    }

    @Operation(summary = "支部详情", deprecated = true,
            description = "已废弃，请改用 GET /api/v4/admin/branches/{id}")
    @Deprecated
    @GetMapping("/{id}")
    public Result<Branch> getById(@PathVariable Long id) {
        return Result.ok(branchService.getById(id));
    }

    @Operation(summary = "新增支部", deprecated = true,
            description = "已废弃，请改用 POST /api/v4/admin/branches（含名称唯一与书记必填校验）")
    @Deprecated
    @PostMapping
    public Result<?> add(@RequestBody Branch branch) {
        branchService.save(branch);
        return Result.ok();
    }

    @Operation(summary = "编辑支部", deprecated = true,
            description = "已废弃，请改用 PUT /api/v4/admin/branches/{id}（支持部分更新）")
    @Deprecated
    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody Branch branch) {
        branch.setId(id);
        branchService.update(branch);
        return Result.ok();
    }

    @Operation(summary = "删除支部（软删除）", deprecated = true,
            description = "已废弃，请改用 DELETE /api/v4/admin/branches/{id}")
    @Deprecated
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        branchService.delete(id);
        return Result.ok();
    }
}
