package sicau.policialPartyManager.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sicau.policialPartyManager.api.dto.CultivationProfileVo;
import sicau.policialPartyManager.api.dto.Result;
import sicau.policialPartyManager.config.CurrentUser;
import sicau.policialPartyManager.model.records.User;
import sicau.policialPartyManager.service.Impl.CultivationProfileServiceImpl;

/**
 * 电子培养档案（#80）：一次请求返回完整培养信息。
 * <p>超管全院、支委限本支部、普通成员只能看自己；越权统一「用户不存在」。
 * <p>只出 JSON，导出 PDF 由前端排版打印。
 * <p>注意：{@code /me} 是固定字面量路径，必须声明为 {@code @GetMapping("/me")}
 * 而不是依赖 {@code /{userId}} 的匹配顺序——否则会先命中变量路径并抛参数类型错误。
 */
@Tag(name = "电子培养档案", description = "完整培养信息聚合查询")
@RestController
@RequestMapping("/api/v4/cultivation-profiles")
@RequiredArgsConstructor
public class CultivationProfileController {

    private final CultivationProfileServiceImpl profileService;

    @Operation(summary = "查询自己的完整培养档案")
    @GetMapping("/me")
    public Result<CultivationProfileVo> me(@CurrentUser User operator) {
        return Result.ok(profileService.of(operator, operator.userId()));
    }

    @Operation(summary = "查询成员完整培养档案",
            description = "基本信息 + 培养联系人 + 身份变更历史 + 思想汇报 + 活动签到/请假 + 学习记录 + 培养材料 + 统计摘要；"
                    + "各区块无数据时返回空集合")
    @GetMapping("/{userId}")
    public Result<CultivationProfileVo> profile(@CurrentUser User operator, @PathVariable Long userId) {
        return Result.ok(profileService.of(operator, userId));
    }
}
