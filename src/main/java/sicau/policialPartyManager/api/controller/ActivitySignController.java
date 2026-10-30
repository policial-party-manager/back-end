package sicau.policialPartyManager.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sicau.policialPartyManager.api.dto.Result;
import sicau.policialPartyManager.api.dto.SignInRequest;
import sicau.policialPartyManager.config.CurrentUser;
import sicau.policialPartyManager.model.entity.SignRecord;
import sicau.policialPartyManager.model.records.User;
import sicau.policialPartyManager.service.SignRecordService;

/**
 * 活动签到（成员侧）：仅登录用户本人签到。
 * 路径前缀沿用 v4（项目约定：不新增版本前缀，新端点并入所属域）。
 */
@Tag(name = "活动签到", description = "成员对已发布活动在签到窗内签到")
@RestController
@RequestMapping("/api/v4/activities")
@RequiredArgsConstructor
public class ActivitySignController {

    private final SignRecordService signRecordService;

    @Operation(summary = "活动签到",
            description = "校验活动已发布、在签到窗内、活动类型需要签到且未重复签到；"
                    + "device/location(WKT)/remark 可空")
    @PostMapping("/{activityId}/sign")
    public Result<SignRecord> sign(@PathVariable Long activityId,
                                   @Valid @RequestBody SignInRequest request,
                                   @CurrentUser User user) {
        request.setActivityId(activityId);
        return Result.ok(signRecordService.sign(request, user.userId()));
    }
}
