package sicau.policialPartyManager.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 身份变更消息实体
 * 对应数据库表：tb_identity_change
 *
 * @author sicau
 */
@Data
@TableName("tb_identity_change")
public class IdentityChange {

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
     * 变更前身份（关联 tb_identity.id）
     */
    private Long fromIdentity;

    /**
     * 变更后身份（关联 tb_identity.id）
     */
    private Long toIdentity;

    /**
     * 操作者id（关联 tb_user.id）
     */
    private Long operatorId;

    /**
     * 变更原因
     */
    private String reason;

    /**
     * 变更时间
     */
    private LocalDateTime createTime;
}
