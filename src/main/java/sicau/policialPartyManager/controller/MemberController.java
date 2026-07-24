package sicau.policialPartyManager.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sicau.policialPartyManager.common.PageResult;
import sicau.policialPartyManager.common.Result;
import sicau.policialPartyManager.config.CurrentUser;
import sicau.policialPartyManager.config.JwtAuthFilter.TokenUser;
import sicau.policialPartyManager.entity.Member;
import sicau.policialPartyManager.service.MemberService;

@Tag(name = "成员管理", description = "党建成员的增删改查（分页、权限隔离）")
@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "成员列表（分页）")
    @GetMapping("/list")
    public Result<PageResult<Member>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "姓名/学号搜索") @RequestParam(required = false) String keyword,
            @Parameter(description = "学院筛选") @RequestParam(required = false) String college,
            @Parameter(description = "政治身份ID筛选") @RequestParam(required = false) Long identityId,
            @CurrentUser TokenUser user) {
        return Result.ok(PageResult.of(memberService.page(page, size, keyword, college, identityId, user)));
    }

    @Operation(summary = "成员详情")
    @GetMapping("/{id}")
    public Result<Member> getById(
            @Parameter(description = "学号") @PathVariable String id,
            @CurrentUser TokenUser user) {
        return Result.ok(memberService.getById(id, user));
    }

    @Operation(summary = "新增成员")
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_ADMIN')")
    public Result<?> add(@RequestBody Member member) {
        memberService.save(member);
        return Result.ok();
    }

    @Operation(summary = "编辑成员")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_ADMIN')")
    public Result<?> update(
            @Parameter(description = "学号") @PathVariable String id,
            @RequestBody Member member) {
        member.setStudentNo(id);
        memberService.update(member);
        return Result.ok();
    }

    @Operation(summary = "删除成员（软删除）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_ADMIN')")
    public Result<?> delete(
            @Parameter(description = "学号") @PathVariable String id,
            @CurrentUser TokenUser user) {
        memberService.delete(id, user);
        return Result.ok();
    }
}
