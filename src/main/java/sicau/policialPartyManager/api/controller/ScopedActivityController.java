package sicau.policialPartyManager.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sicau.policialPartyManager.api.dto.ActivitySaveRequest;
import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.api.dto.Result;
import sicau.policialPartyManager.config.CurrentUser;
import sicau.policialPartyManager.model.entity.Activity;
import sicau.policialPartyManager.model.entity.ActivitySignup;
import sicau.policialPartyManager.model.entity.SignRecord;
import sicau.policialPartyManager.model.records.User;
import sicau.policialPartyManager.service.AdminActivityService;
import sicau.policialPartyManager.service.ActivitySignupService;
import sicau.policialPartyManager.service.Impl.ActivityCopyService;
import sicau.policialPartyManager.service.Impl.ActivityScopeGuard;
import sicau.policialPartyManager.service.Impl.SignSheetExportService;
import sicau.policialPartyManager.service.SignRecordService;

import java.nio.charset.StandardCharsets;

/**
 * 支委可用的活动域端点（#71 / #68 补齐）。
 * <p>票面验收判据要求支委能：创建本支部活动、编辑本支部尚未开始的活动、
 * 查看报名与签到、导出签到名单、生成签到二维码。
 * <p>支委**不能**：删除活动、查看全院活动——这些仍是超管专属（留在 AdminController）。
 * <p>所有端点先过 {@link ActivityScopeGuard}：角色不足 403，越支部按「活动不存在」。
 */
@Tag(name = "活动域（支委）", description = "本支部活动的创建/编辑/报名签到查询/导出")
@RestController
@RequestMapping("/api/v4/activities")
@RequiredArgsConstructor
public class ScopedActivityController {

    private final ActivityScopeGuard activityScopeGuard;
    private final AdminActivityService adminActivityService;
    private final ActivitySignupService activitySignupService;
    private final SignRecordService signRecordService;
    private final SignSheetExportService signSheetExportService;
    private final ActivityCopyService activityCopyService;

    // ---------- 写 ----------

    @Operation(summary = "创建活动（支委）",
            description = "支委创建的活动强制归属本支部；超管可指定任意支部")
    @PostMapping
    public Result<Void> create(@CurrentUser User operator, @Valid @RequestBody ActivitySaveRequest request) {
        adminActivityService.createActivityScoped(operator, request);
        return Result.ok();
    }

    @Operation(summary = "编辑活动（支委）",
            description = "支委只能编辑本支部、且尚未开始的活动；已开始或跨支部按「活动不存在」")
    @PutMapping("/{id}")
    public Result<Void> update(@CurrentUser User operator, @PathVariable Long id,
                               @Valid @RequestBody ActivitySaveRequest request) {
        adminActivityService.updateActivityScoped(operator, id, request);
        return Result.ok();
    }

    // ---------- 读 ----------

    @Operation(summary = "活动详情（按操作者范围）")
    @GetMapping("/{activityId}")
    public Result<sicau.policialPartyManager.api.dto.ActivityVo> detail(@CurrentUser User operator,
                                                                        @PathVariable Long activityId) {
        Activity activity = activityScopeGuard.requireManageable(operator, activityId);
        return Result.ok(adminActivityService.getActivity(activity.getId()));
    }

    @Operation(summary = "活动报名记录分页（按操作者范围）")
    @GetMapping("/{activityId}/signups/page")
    public Result<PageResult<ActivitySignup>> signups(@CurrentUser User operator,
                                                      @PathVariable Long activityId,
                                                      @RequestParam(defaultValue = "1") long page,
                                                      @RequestParam(defaultValue = "10") long size,
                                                      @RequestParam(required = false) Long userId) {
        activityScopeGuard.requireManageable(operator, activityId);
        return Result.ok(activitySignupService.pageByActivity(page, size, activityId, userId));
    }

    @Operation(summary = "活动签到记录分页（按操作者范围）")
    @GetMapping("/{activityId}/signs/page")
    public Result<PageResult<SignRecord>> signs(@CurrentUser User operator,
                                                @PathVariable Long activityId,
                                                @RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) Long userId) {
        activityScopeGuard.requireManageable(operator, activityId);
        return Result.ok(signRecordService.pageByActivity(page, size, activityId, userId));
    }

    // ---------- 复制 ----------

    @Operation(summary = "复制活动",
            description = "把上一期活动复制成新草稿：主体/详情/参与范围一并复制，时间整体后移；"
                    + "不复制签到、报名与请假记录。支委限本支部，新活动归属同支部且为未发布状态")
    @PostMapping("/{activityId}/copy")
    public Result<Long> copy(@CurrentUser User operator, @PathVariable Long activityId,
                             @RequestParam(required = false) Integer shiftDays) {
        Activity activity = activityScopeGuard.requireManageable(operator, activityId);
        return Result.ok(activityCopyService.copy(activity.getId(), operator.userId(), shiftDays));
    }

    // ---------- 导出 ----------

    @Operation(summary = "导出签到名单（xlsx）",
            description = "支委限本支部活动；含签到时间与考勤四态，便于线下存档")
    @GetMapping("/{activityId}/signs/export")
    public ResponseEntity<byte[]> exportSigns(@CurrentUser User operator,
                                              @PathVariable Long activityId) {
        Activity activity = activityScopeGuard.requireManageable(operator, activityId);
        byte[] body = signSheetExportService.export(activityId);
        String fileName = "signs-" + activityId + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(fileName, StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }
}
