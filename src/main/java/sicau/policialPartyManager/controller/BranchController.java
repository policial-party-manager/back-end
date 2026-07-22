package sicau.policialPartyManager.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sicau.policialPartyManager.common.Result;
import sicau.policialPartyManager.entity.Branch;
import sicau.policialPartyManager.service.BranchService;

import java.util.List;

@RestController
@RequestMapping("/api/branch")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class BranchController {

    private final BranchService branchService;

    @GetMapping("/list")
    public Result<List<Branch>> list() {
        return Result.ok(branchService.listAll());
    }

    @GetMapping("/{id}")
    public Result<Branch> getById(@PathVariable Long id) {
        return Result.ok(branchService.getById(id));
    }

    @PostMapping
    public Result<?> add(@RequestBody Branch branch) {
        branchService.save(branch);
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody Branch branch) {
        branch.setId(id);
        branchService.update(branch);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        branchService.delete(id);
        return Result.ok();
    }
}
