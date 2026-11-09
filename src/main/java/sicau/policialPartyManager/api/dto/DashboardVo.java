package sicau.policialPartyManager.api.dto;

import lombok.Data;

import java.util.List;

/**
 * 首页统计（#81）。
 * <p>只做能驱动动作的数：待审核思想汇报、待审批身份变更、阶段分布。
 * 纯展示类数字（本月新增党员数、本周活动数）本次不做。
 */
@Data
public class DashboardVo {

    /** 待审核思想汇报数（支委限本支部、超管全院） */
    private long pendingThoughtReports;

    /** 待审批身份变更数（支委限本支部、超管全院） */
    private long pendingIdentityChanges;

    /** 各政治身份人数分布 */
    private List<IdentityCount> identityDistribution;

    @Data
    public static class IdentityCount {
        private Long identityId;
        private String identityName;
        private Integer level;
        private long count;
    }
}
