package sicau.policialPartyManager.service;

import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.api.dto.ThoughtReportSubmitRequest;
import sicau.policialPartyManager.api.dto.ThoughtReportVo;
import sicau.policialPartyManager.api.dto.ThoughtReviewRequest;
import sicau.policialPartyManager.model.records.User;

/**
 * 思想汇报提交与审阅流（#33）。
 * <p>
 * 状态机：1 待审阅 / 2 通过 / 3 驳回；驳回后重提生成新行，旧行留痕。
 * 可见性：成员只见自己的；审阅人只见指派给自己的（越权统一「不存在」语义，不泄露存在性）。
 */
public interface ThoughtReportService {

    /** 成员提交；审阅人自动派定为所属支部秘书 */
    ThoughtReportVo submit(ThoughtReportSubmitRequest request, User user);

    /** 成员：我的汇报分页（可按状态过滤，不含正文） */
    PageResult<ThoughtReportVo> pageMine(long page, long size, Integer status, User user);

    /** 成员：撤回未审阅的汇报（已审阅不可撤回） */
    void deleteMine(Long id, User user);

    /** 审阅人：待我审阅分页（可按状态过滤，不含正文） */
    PageResult<ThoughtReportVo> pageForReviewer(long page, long size, Integer status, User reviewer);

    /** 审阅人：查看某条汇报详情（含正文） */
    ThoughtReportVo getForReviewer(Long id, User reviewer);

    /** 审阅人：通过/驳回 */
    ThoughtReportVo review(Long id, ThoughtReviewRequest request, User reviewer);

    /** 超管：全局分页（秘书缺岗时兜底查看，只读） */
    PageResult<ThoughtReportVo> pageAll(long page, long size, Integer status, Long userId);
}
