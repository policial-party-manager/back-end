package sicau.policialPartyManager.api.dto;

import lombok.Data;

import java.util.List;

/**
 * 阶段分布（#81）：按学院/专业/班级/年级任一维度切分的各政治身份人数。
 */
@Data
public class DistributionVo {

    /** 切分维度：college / major / className / grade */
    private String dimension;

    /** 每个分组下的各身份人数 */
    private List<Group> groups;

    @Data
    public static class Group {
        /** 分组值（维度取值，如 软件学院） */
        private String key;
        /** 该组总人数 */
        private long total;
        /** 该组各身份人数 */
        private List<DashboardVo.IdentityCount> identities;
    }
}
