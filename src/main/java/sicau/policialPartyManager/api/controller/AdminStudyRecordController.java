package sicau.policialPartyManager.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.api.dto.Result;
import sicau.policialPartyManager.api.dto.StudyRecordUpsertRequest;
import sicau.policialPartyManager.api.dto.StudyRecordVo;
import sicau.policialPartyManager.config.CurrentUser;
import sicau.policialPartyManager.model.records.User;
import sicau.policialPartyManager.service.StudyRecordService;

/**
 * 学习记录管理侧（#32）：超管全局读写；支部管理员限自己担任秘书的支部。
 * 作用域在服务层强制，越界统一「用户/记录不存在」语义。
 */
@Tag(name = "管理员后台-学习记录", description = "学习记录录入/删除/查询（超管全局，支管限本支部）")
@RestController
@RequestMapping("/api/v4/admin/study-records")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','BRANCH_ADMIN')")
@RequiredArgsConstructor
public class AdminStudyRecordController {

    private final StudyRecordService studyRecordService;

    @Operation(summary = "录入学习记录",
            description = "一人一课一行：重复提交=覆盖更新；branch_admin 只能录入自己担任秘书的支部成员")
    @PostMapping
    public Result<StudyRecordVo> upsert(@Valid @RequestBody StudyRecordUpsertRequest request,
                                        @CurrentUser User operator) {
        return Result.ok(studyRecordService.upsert(request, operator));
    }

    @Operation(summary = "删除学习记录", description = "同录入权限面；支管越界按「记录不存在」处理")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, @CurrentUser User operator) {
        studyRecordService.delete(id, operator);
        return Result.ok();
    }

    @Operation(summary = "学习记录分页（管理侧）",
            description = "超管可按 userId/courseId/courseType 过滤全局；支管只看本支部成员（userId 可选过滤）")
    @GetMapping("/page")
    public Result<PageResult<StudyRecordVo>> page(@RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "10") long size,
                                                  @RequestParam(required = false) Long userId,
                                                  @RequestParam(required = false) Long courseId,
                                                  @RequestParam(required = false) String courseType,
                                                  @CurrentUser User operator) {
        if ("super_admin".equalsIgnoreCase(operator.role())) {
            return Result.ok(studyRecordService.pageForAdmin(page, size, userId, courseId, courseType));
        }
        return Result.ok(studyRecordService.pageForBranch(page, size, userId, operator));
    }
}
