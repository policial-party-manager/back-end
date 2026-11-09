package sicau.policialPartyManager.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sicau.policialPartyManager.api.dto.BatchResult;
import sicau.policialPartyManager.api.dto.Result;
import sicau.policialPartyManager.config.CurrentUser;
import sicau.policialPartyManager.model.entity.CultivationMaterial;
import sicau.policialPartyManager.model.records.User;
import sicau.policialPartyManager.service.Impl.CultivationMaterialServiceImpl;
import sicau.policialPartyManager.service.Impl.MentorServiceImpl;

import java.util.List;

/**
 * 培养域：培养联系人（#72）与培养材料（#73）。
 * <p>培养联系人只能由组织指派，成员个人中心没有写入口。
 */
@Tag(name = "培养域", description = "培养联系人指派与培养材料管理")
@RestController
@RequestMapping("/api/v4/members/{userId}")
@RequiredArgsConstructor
public class CultivationController {

    private final MentorServiceImpl mentorService;
    private final CultivationMaterialServiceImpl materialService;
    private final sicau.policialPartyManager.service.BranchScopeService branchScopeService;

    // ---------- 培养联系人 ----------

    @Operation(summary = "查看成员当前培养联系人", description = "按操作者范围；越权与真不存在同文案")
    @GetMapping("/mentors")
    public Result<List<MentorServiceImpl.MentorView>> mentors(@CurrentUser User operator,
                                                             @PathVariable Long userId) {
        // 与 materials() 同策略：培养关系属成员隐私，必须过支部范围闸门
        branchScopeService.requireManageableUser(operator, userId);
        return Result.ok(mentorService.mentorViewsOf(userId));
    }

    @Operation(summary = "指派培养联系人", description = "每人最多 2 名在职联系人；已存在则幂等")
    @PostMapping("/mentors")
    public Result<Void> assignMentor(@CurrentUser User operator, @PathVariable Long userId,
                                     @Valid @RequestBody MentorAssignRequest request) {
        mentorService.assign(operator, userId, request.getMentorId());
        return Result.ok();
    }

    @Operation(summary = "移除培养联系人")
    @DeleteMapping("/mentors/{mentorId}")
    public Result<Void> removeMentor(@CurrentUser User operator, @PathVariable Long userId,
                                     @PathVariable Long mentorId) {
        mentorService.remove(operator, userId, mentorId);
        return Result.ok();
    }

    @Operation(summary = "按培养联系人反查带教成员", description = "支委只返回本支部成员")
    @GetMapping("/mentees")
    public Result<List<Long>> mentees(@CurrentUser User operator,
                                      @RequestParam Long mentorId) {
        return Result.ok(mentorService.menteesOf(operator, mentorId));
    }

    @Operation(summary = "批量指派培养联系人", description = "逐条成功；单次最多 200 条")
    @PostMapping("/mentors/batch")
    public Result<BatchResult> batchAssign(@CurrentUser User operator,
                                           @Valid @RequestBody MentorBatchRequest request) {
        return Result.ok(mentorService.batchAssign(operator, request.getUserIds(), request.getMentorId()));
    }

    // ---------- 培养材料 ----------

    @Operation(summary = "成员培养材料列表")
    @GetMapping("/materials")
    public Result<List<CultivationMaterial>> materials(@CurrentUser User operator,
                                                       @PathVariable Long userId) {
        return Result.ok(materialService.listOf(operator, userId));
    }

    @Operation(summary = "登记培养材料",
            description = "先调 /api/v4/uploads 拿到 file: 路径，再用本接口登记归属与类型")
    @PostMapping("/materials")
    public Result<Long> addMaterial(@CurrentUser User operator, @PathVariable Long userId,
                                    @Valid @RequestBody MaterialCreateRequest request) {
        return Result.ok(materialService.upload(operator, userId, request.getMaterialType(),
                request.getFile(), request.getFileName(), request.getRemark()));
    }

    @Operation(summary = "删除培养材料（软删除）")
    @DeleteMapping("/materials/{materialId}")
    public Result<Void> removeMaterial(@CurrentUser User operator, @PathVariable Long userId,
                                       @PathVariable Long materialId) {
        materialService.remove(operator, materialId);
        return Result.ok();
    }

    @Data
    public static class MentorAssignRequest {
        @NotNull(message = "培养联系人不能为空")
        private Long mentorId;
    }

    @Data
    public static class MentorBatchRequest {
        @NotEmpty(message = "请至少选择一条记录")
        @Size(max = 200, message = "单次批量最多 200 条，请分批操作")
        private List<Long> userIds;
        @NotNull(message = "培养联系人不能为空")
        private Long mentorId;
    }

    @Data
    public static class MaterialCreateRequest {
        @NotNull(message = "材料类型不能为空")
        private String materialType;
        @NotNull(message = "文件地址不能为空")
        private String file;
        private String fileName;
        @Size(max = 200, message = "备注长度不能超过 200")
        private String remark;
    }
}
