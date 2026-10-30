package sicau.policialPartyManager.api.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 身份变更单视图（内嵌人物/身份/操作者/审批人姓名，列表批量回查防 N+1）。
 */
@Data
public class IdentityChangeVo {

    private Long id;

    private Long userId;

    /** 目标用户姓名 */
    private String userName;

    /** 变更前身份id（首次登记为 null） */
    private Long fromIdentity;

    /** 变更前身份名 */
    private String fromIdentityName;

    /** 变更后身份id */
    private Long toIdentity;

    /** 变更后身份名 */
    private String toIdentityName;

    /** 发起操作者id */
    private Long operatorId;

    /** 发起操作者姓名 */
    private String operatorName;

    /** 变更原因 */
    private String reason;

    /** 单据状态：1 待审批 / 2 已生效 / 3 驳回 */
    private Integer status;

    /** 状态中文名 */
    private String statusName;

    /** 审批人id（未审批为 null） */
    private Long reviewerId;

    /** 审批人姓名 */
    private String reviewerName;

    /** 审批时间 */
    private LocalDateTime reviewTime;

    /** 审批意见 */
    private String reviewRemark;

    /** 发起时间 */
    private LocalDateTime createTime;
}
