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
import sicau.policialPartyManager.api.dto.StudyRecordVo;
import sicau.policialPartyManager.api.dto.StudySummaryVo;
import sicau.policialPartyManager.config.CurrentUser;
import sicau.policialPartyManager.model.records.User;
import sicau.policialPartyManager.service.StudyRecordService;

/**
 * 学习记录（成员侧，#32）：只读自己的记录与汇总（triage 决策：成员不写）。
 */
@Tag(name = "个人中心-学习记录", description = "我的学习记录与汇总（只读）")
@RestController
@RequestMapping("/api/v4/user/study-records")
@RequiredArgsConstructor
public class UserStudyRecordController {

    private final StudyRecordService studyRecordService;

    @Operation(summary = "我的学习记录分页", description = "仅本人数据，含课程名/类型")
    @GetMapping("/mine/page")
    public Result<PageResult<StudyRecordVo>> mine(@RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "10") long size,
                                                  @CurrentUser User user) {
        return Result.ok(studyRecordService.pageMine(page, size, user));
    }

    @Operation(summary = "我的学习汇总", description = "条数、总学时（分钟）、平均分")
    @GetMapping("/mine/summary")
    public Result<StudySummaryVo> summary(@CurrentUser User user) {
        return Result.ok(studyRecordService.summaryMine(user));
    }
}
