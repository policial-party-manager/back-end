package sicau.policialPartyManager.service;

import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.api.dto.StudyRecordUpsertRequest;
import sicau.policialPartyManager.api.dto.StudyRecordVo;
import sicau.policialPartyManager.api.dto.StudySummaryVo;
import sicau.policialPartyManager.model.records.User;

/**
 * 学习记录服务（#32）。
 * <p>
 * 权限面（triage 决策：管理员录入，成员只读）：
 * super_admin 全局读写；branch_admin 限「自己担任秘书的支部」的成员读写，
 * 目标越出范围统一「用户不存在」语义（不泄露他支部人员存在性）。
 * 一人一课一行：重复提交 = 覆盖更新。
 */
public interface StudyRecordService {

    /** 录入（upsert）；返回带学生/课程姓名的视图 */
    StudyRecordVo upsert(StudyRecordUpsertRequest request, User operator);

    /** 删除记录；同写入权限面 */
    void delete(Long id, User operator);

    /** 成员：我的学习记录分页 */
    PageResult<StudyRecordVo> pageMine(long page, long size, User user);

    /** 成员：我的汇总（条数/总分钟/均分） */
    StudySummaryVo summaryMine(User user);

    /** 支管/超管：本支部成员的学习记录分页（userId 过滤可选） */
    PageResult<StudyRecordVo> pageForBranch(long page, long size, Long userId, User operator);

    /** 超管：全局分页（userId/courseId/courseType 过滤可选） */
    PageResult<StudyRecordVo> pageForAdmin(long page, long size, Long userId, Long courseId, String courseType);
}
