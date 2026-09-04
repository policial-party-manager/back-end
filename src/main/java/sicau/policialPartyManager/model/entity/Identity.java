package sicau.policialPartyManager.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 政治身份实体
 * 对应数据库表：tb_identity
 *
 * @author sicau
 */
@Data
@TableName("tb_identity")
public class Identity {

    /**
     * 身份ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 身份名
     */
    private String identityName;

    /**
     * 发展阶段（1-6）
     */
    private Integer level;

    /**
     * 对应角色id（关联 tb_role.id）
     */
    private Long roleId;

    /**
     * 说明
     */
    private String description;
}
