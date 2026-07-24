package sicau.policialPartyManager.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import sicau.policialPartyManager.common.Result;
import sicau.policialPartyManager.config.CurrentUser;
import sicau.policialPartyManager.config.JwtAuthFilter.TokenUser;
import sicau.policialPartyManager.entity.Member;
import sicau.policialPartyManager.entity.User;
import sicau.policialPartyManager.repository.MemberMapper;
import sicau.policialPartyManager.repository.UserMapper;

import java.util.Map;

@Tag(name = "个人中心", description = "当前用户个人信息查看与编辑")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;
    private final MemberMapper memberMapper;

    @Operation(summary = "获取个人信息")
    @GetMapping("/profile")
    public Result<?> profile(@CurrentUser TokenUser user) {
        User u = userMapper.selectById(user.userId());
        Member member = memberMapper.selectOne(
                new LambdaQueryWrapper<Member>().eq(Member::getStudentNo, user.username()));

        return Result.ok(Map.of(
                "user", u,
                "member", member
        ));
    }

    @Operation(summary = "编辑个人信息")
    @PutMapping("/profile")
    public Result<?> updateProfile(@RequestBody Map<String, Object> body, @CurrentUser TokenUser user) {
        Member member = memberMapper.selectOne(
                new LambdaQueryWrapper<Member>().eq(Member::getStudentNo, user.username()));
        if (member == null) return Result.fail("未找到成员信息");

        if (body.containsKey("phone")) member.setPhone((String) body.get("phone"));
        if (body.containsKey("email")) member.setEmail((String) body.get("email"));
        memberMapper.updateById(member);
        return Result.ok();
    }
}
