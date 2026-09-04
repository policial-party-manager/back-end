package sicau.policialPartyManager.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色表实体
 * 对应数据库表：tb_role
 *
 * @author sicau
 */
@Data
@TableName("tb_role")
public class Role {

    /**
     * 角色ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 角色名称（需以 ROLE_ 开头）
     */
    private String roleName;

    /**
     * 角色描述
     */
    private String description;
}
