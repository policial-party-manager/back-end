package sicau.policialPartyManager.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sicau.policialPartyManager.common.Result;
import sicau.policialPartyManager.entity.Branch;
import sicau.policialPartyManager.service.BranchService;

import java.util.List;

@Tag(name = "党支部管理", description = "党支部的增删改查")
@RestController
@RequestMapping("/api/branch")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class BranchController {

    private final BranchService branchService;

    @Operation(summary = "支部列表")
    @GetMapping("/list")
    public Result<List<Branch>> list() {
        return Result.ok(branchService.listAll());
    }

    @Operation(summary = "支部详情")
    @GetMapping("/{id}")
    public Result<Branch> getById(@PathVariable Long id) {
        return Result.ok(branchService.getById(id));
    }

    @Operation(summary = "新增支部")
    @PostMapping
    public Result<?> add(@RequestBody Branch branch) {
        branchService.save(branch);
        return Result.ok();
    }

    @Operation(summary = "编辑支部")
    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody Branch branch) {
        branch.setId(id);
        branchService.update(branch);
        return Result.ok();
    }

    @Operation(summary = "删除支部（软删除）")
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        branchService.delete(id);
        return Result.ok();
    }
}
