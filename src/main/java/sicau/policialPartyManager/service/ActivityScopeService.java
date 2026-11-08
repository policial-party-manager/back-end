package sicau.policialPartyManager.service;

import sicau.policialPartyManager.model.entity.ActivityIdentity;

import java.util.List;

/**
 * 活动参与范围判定（单一口径）。
 * <p>报名、签到、活动统计（应到人数）三处共用本组件，避免各写一份导致口径漂移。
 * <p>规则：命中任意一行目标人群即通过；无任何范围行 = 全场开放；
 * 行上为空的维度不参与校验（学院空=不限、年级空=不限、身份空=不限）。
 */
public interface ActivityScopeService {

    /** 读取活动的目标人群行；活动无任何范围行时返回空列表（=全场开放）。 */
    List<ActivityIdentity> rowsOf(Long activityId);

    /**
     * 判定用户是否属于活动参与范围。
     * <p>不在范围内时抛出 {@link IllegalArgumentException}，文案与报名侧一致；
     * 受限身份活动且用户未登记政治身份时，给出可执行提示。
     */
    void requireEligible(Long activityId, Long userId);

    /**
     * 判定用户是否属于活动参与范围（不抛异常）。
     *
     * @return true = 属于或活动未设范围
     */
    boolean isEligible(Long activityId, Long userId);

    /**
     * 应到人数：参与范围匹配到的成员总数；活动无范围行时返回 null（表示「全场开放、应到不可界定」）。
     */
    Integer expectedCount(Long activityId);
}
