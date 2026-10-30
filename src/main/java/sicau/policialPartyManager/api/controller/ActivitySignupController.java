package sicau.policialPartyManager.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sicau.policialPartyManager.api.dto.Result;
import sicau.policialPartyManager.config.CurrentUser;
import sicau.policialPartyManager.model.entity.ActivitySignup;
import sicau.policialPartyManager.model.records.User;
import sicau.policialPartyManager.service.ActivitySignupService;

/**
 * 活动报名（成员侧）：报名即生效，活动开始前可取消。
 * 路径与签到同挂在 /api/v4/activities 资源下（v4 前缀冻结，不新增版本）。
 */
@Tag(name = "活动报名", description = "成员报名/取消活动（报名即生效，活动开始前可取消）")
@RestController
@RequestMapping("/api/v4/activities")
@RequiredArgsConstructor
public class ActivitySignupController {

    private final ActivitySignupService activitySignupService;

    @Operation(summary = "活动报名",
            description = "已发布且未开始的活动可报名；校验重复报名、名额（maxParticipants，空=不限）"
                    + "与目标人群范围（年级/学院；身份维度本期不校验）")
    @PostMapping("/{activityId}/signup")
    public Result<ActivitySignup> signup(@PathVariable Long activityId, @CurrentUser User user) {
        return Result.ok(activitySignupService.signup(activityId, user.userId()));
    }

    @Operation(summary = "取消报名", description = "活动开始前可取消；记录删除即名额释放")
    @DeleteMapping("/{activityId}/signup")
    public Result<Void> cancel(@PathVariable Long activityId, @CurrentUser User user) {
        activitySignupService.cancel(activityId, user.userId());
        return Result.ok();
    }
}
