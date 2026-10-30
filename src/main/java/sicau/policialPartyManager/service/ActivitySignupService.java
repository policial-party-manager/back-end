package sicau.policialPartyManager.service;

import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.model.entity.ActivitySignup;

/**
 * 活动报名服务（#53）。
 * <p>
 * 规则：报名即生效（无审核）；已发布且未开始的活动可报名/取消；
 * 名额取活动详情 maxParticipants（空=不限）；范围按 tb_activity_identity
 * 的年级/学院匹配（身份维度本期不校验，见 #44）。
 * 错误统一 IllegalArgumentException（400）。
 */
public interface ActivitySignupService {

    /** 成员报名；返回落库后的报名记录 */
    ActivitySignup signup(Long activityId, Long userId);

    /** 成员取消报名（活动开始前）；记录删除即名额释放 */
    void cancel(Long activityId, Long userId);

    /** 管理员：按活动分页查报名，可按用户过滤 */
    PageResult<ActivitySignup> pageByActivity(long page, long size, Long activityId, Long userId);
}
