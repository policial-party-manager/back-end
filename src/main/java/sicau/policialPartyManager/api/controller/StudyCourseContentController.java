package sicau.policialPartyManager.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.api.dto.Result;
import sicau.policialPartyManager.model.entity.Study;
import sicau.policialPartyManager.service.StudyService;

/**
 * 课程字典只读列表（#32）：登录即可（录入选课/成员查看课程目录）。
 */
@Tag(name = "内容-课程字典", description = "课程列表（登录可读）")
@RestController
@RequestMapping("/api/v2/content/study-courses")
@RequiredArgsConstructor
public class StudyCourseContentController {

    private final StudyService studyService;

    @Operation(summary = "课程分页列表", description = "keyword 匹配课程名，type 精确过滤；所有登录用户可读")
    @GetMapping("/page")
    public Result<PageResult<Study>> page(@RequestParam(defaultValue = "1") long page,
                                          @RequestParam(defaultValue = "10") long size,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) String type) {
        return Result.ok(studyService.page(page, size, keyword, type));
    }
}
