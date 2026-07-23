package sicau.policialPartyManager.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import sicau.policialPartyManager.common.PageResult;
import sicau.policialPartyManager.common.Result;
import sicau.policialPartyManager.config.JwtAuthFilter.TokenUser;
import sicau.policialPartyManager.entity.Member;
import sicau.policialPartyManager.service.MemberService;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/list")
    public Result<PageResult<Member>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String college,
            @RequestParam(required = false) String identity,
            Authentication authentication) {
        TokenUser user = (TokenUser) authentication.getDetails();
        return Result.ok(PageResult.of(memberService.page(page, size, keyword, college, identity, user)));
    }

    @GetMapping("/{id}")
    public Result<Member> getById(@PathVariable Long id, Authentication authentication) {
        TokenUser user = (TokenUser) authentication.getDetails();
        return Result.ok(memberService.getById(id, user));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_ADMIN')")
    public Result<?> add(@RequestBody Member member) {
        memberService.save(member);
        return Result.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_ADMIN')")
    public Result<?> update(@PathVariable Long id, @RequestBody Member member) {
        member.setId(id);
        memberService.update(member);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_ADMIN')")
    public Result<?> delete(@PathVariable Long id, Authentication authentication) {
        TokenUser user = (TokenUser) authentication.getDetails();
        memberService.delete(id, user);
        return Result.ok();
    }
}
