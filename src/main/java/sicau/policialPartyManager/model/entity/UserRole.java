package sicau.policialPartyManager.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户角色关联表实体
 * 对应数据库表：tb_user_role
 *
 * @author sicau
 */
@Data
@TableName("tb_user_role")
public class UserRole {

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
     * 角色id（关联 tb_role.id）
     */
    private Long roleId;
}
