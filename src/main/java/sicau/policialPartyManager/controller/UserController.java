package sicau.policialPartyManager.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import sicau.policialPartyManager.common.Result;
import sicau.policialPartyManager.config.JwtAuthFilter.TokenUser;
import sicau.policialPartyManager.entity.Member;
import sicau.policialPartyManager.entity.User;
import sicau.policialPartyManager.repository.MemberMapper;
import sicau.policialPartyManager.repository.UserMapper;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;
    private final MemberMapper memberMapper;

    @GetMapping("/profile")
    public Result<?> profile(Authentication authentication) {
        TokenUser tu = (TokenUser) authentication.getDetails();
        User user = userMapper.selectById(tu.userId());
        Member member = memberMapper.selectOne(
                new LambdaQueryWrapper<Member>().eq(Member::getStudentNo, tu.username()));

        return Result.ok(Map.of(
                "user", user,
                "member", member
        ));
    }

    @PutMapping("/profile")
    public Result<?> updateProfile(@RequestBody Map<String, Object> body, Authentication authentication) {
        TokenUser tu = (TokenUser) authentication.getDetails();
        Member member = memberMapper.selectOne(
                new LambdaQueryWrapper<Member>().eq(Member::getStudentNo, tu.username()));
        if (member == null) return Result.fail("未找到成员信息");

        if (body.containsKey("phone")) member.setPhone((String) body.get("phone"));
        if (body.containsKey("email")) member.setEmail((String) body.get("email"));
        memberMapper.updateById(member);
        return Result.ok();
    }
}
