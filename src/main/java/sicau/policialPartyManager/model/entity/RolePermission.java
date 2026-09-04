package sicau.policialPartyManager.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色权限关联表实体
 * 对应数据库表：tb_role_permission
 *
 * @author sicau
 */
@Data
@TableName("tb_role_permission")
public class RolePermission {

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
     */
    private String permissionName;
}
