package sicau.policialPartyManager.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
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
import sicau.policialPartyManager.service.Impl.SignQrTokenService;
import sicau.policialPartyManager.service.SignRecordService;

/**
 * 二维码签到（#79）。
 * <p>支委/超管生成时效二维码，成员扫码完成签到。token 默认 60 秒失效，
 * 一张有效二维码可被现场所有参与者扫——它证明「此刻在场」，不是一人一码。
 * <p>不做地理围栏。
 */
@Tag(name = "二维码签到", description = "生成时效签到码与扫码签到")
@RestController
@RequestMapping("/api/v4/activities")
@RequiredArgsConstructor
public class SignQrController {

    private final SignQrTokenService tokenService;
    private final SignRecordService signRecordService;

    @Operation(summary = "生成/获取当前签到二维码",
            description = "返回 token 与过期秒数；同一窗口内复用同一 token，过期后自动轮换")
    @GetMapping("/{activityId}/sign-qr")
    public Result<QrVo> issue(@CurrentUser User operator, @PathVariable Long activityId) {
        return Result.ok(new QrVo(tokenService.issue(activityId), tokenService.ttlSeconds()));
    }

    @Operation(summary = "扫码签到",
            description = "携带当前有效 token；过期或伪造被拒。重复签到返回「您已完成签到」")
    @PostMapping("/{activityId}/sign-by-qr")
    public Result<SignRecord> signByQr(@CurrentUser User operator, @PathVariable Long activityId,
                                       @Valid @RequestBody QrSignRequest request) {
        if (!tokenService.verify(activityId, request.getToken())) {
            throw new IllegalArgumentException("二维码已过期，请扫描最新二维码");
        }
        SignInRequest signRequest = new SignInRequest();
        signRequest.setActivityId(activityId);
        signRequest.setDevice(request.getDevice() == null ? "mini" : request.getDevice());
        return Result.ok(signRecordService.sign(signRequest, operator.userId()));
    }

    @Data
    public static class QrVo {
        private final String token;
        private final long expiresInSeconds;

        public QrVo(String token, long expiresInSeconds) {
            this.token = token;
            this.expiresInSeconds = expiresInSeconds;
        }
    }

    @Data
    public static class QrSignRequest {
        @NotNull(message = "二维码 token 不能为空")
        private String token;
        private String device;
    }
}
