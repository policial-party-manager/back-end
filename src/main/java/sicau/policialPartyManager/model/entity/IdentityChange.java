package sicau.policialPartyManager.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 政治身份变更单实体（两阶段：支管/超管发起 -> 超管审批；生效才写回 identity_id）
 * 对应数据库表：tb_identity_change
 *
 * @author sicau
 */
@Data
@TableName("tb_identity_change")
public class IdentityChange {

    /** 状态：待审批 */
    public static final int STATUS_PENDING = 1;
    /** 状态：已生效 */
    public static final int STATUS_APPROVED = 2;
    /** 状态：驳回 */
    public static final int STATUS_REJECTED = 3;

    /**
     * 主键ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户id（关联 tb_user.id）
     */
    private Long userId;

    /**
     * 变更前身份（关联 tb_identity.id；首次登记可为 null）
     */
    private Long fromIdentity;

    /**
     * 变更后身份（关联 tb_identity.id）
     */
    private Long toIdentity;

    /**
     * 发起操作者id（关联 tb_user.id）
     */
    private Long operatorId;

    /**
     * 变更原因
     */
    private String reason;

    /**
     * 单据状态（1：待审批，2：已生效，3：驳回）
     */
    private Integer status;

    /**
     * 审批人（仅 super_admin 审批；关联 tb_user.id）
     */
    private Long reviewerId;

    /**
     * 审批时间
     */
    private LocalDateTime reviewTime;

    /**
     * 审批意见
     */
    private String reviewRemark;

    /**
     * 发起时间
     */
    private LocalDateTime createTime;
}
