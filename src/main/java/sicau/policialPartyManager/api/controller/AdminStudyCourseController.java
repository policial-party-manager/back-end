package sicau.policialPartyManager.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sicau.policialPartyManager.api.dto.Result;
import sicau.policialPartyManager.api.dto.StudySaveRequest;
import sicau.policialPartyManager.model.entity.Study;
import sicau.policialPartyManager.service.StudyService;

/**
 * 课程字典维护（#32）：仅超级管理员。读列表在内容侧（登录即可）。
 */
@Tag(name = "管理员后台-课程字典", description = "学习课程字典的增删改（超管）")
@RestController
@RequestMapping("/api/v4/admin/study-courses")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminStudyCourseController {

    private final StudyService studyService;

    @Operation(summary = "新增课程", description = "名称全表唯一；类型自由文本（如 必修/选修/专题）")
    @PostMapping
    public Result<Study> create(@Valid @RequestBody StudySaveRequest request) {
        return Result.ok(studyService.create(request));
    }

    @Operation(summary = "编辑课程", description = "重名（不含自身）拒绝")
    @PutMapping("/{id}")
    public Result<Study> update(@PathVariable Long id, @Valid @RequestBody StudySaveRequest request) {
        return Result.ok(studyService.update(id, request));
    }

    @Operation(summary = "删除课程", description = "已有学习记录引用的课程不可删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        studyService.delete(id);
        return Result.ok();
    }
}
