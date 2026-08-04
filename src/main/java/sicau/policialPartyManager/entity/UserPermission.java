package sicau.policialPartyManager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户权限关联表实体
 * 对应数据库表：tb_user_permission
 *
 * @author sicau
 */
@Data
@TableName("tb_user_permission")
public class UserPermission {

    /**
     * 主键ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 角色id（关联 tb_role.id）
     */
    private Long roleId;

    /**
     * 权限名称
     * 注意：数据库列名为 permission_nmae（原 SQL 存在拼写错误）
     */
    @TableField("permission_nmae")
    private String permissionName;
}
