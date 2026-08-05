package sicau.policialPartyManager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表实体
 * 对应数据库表：tb_user
 *
 * @author sicau
 */
@Data
@TableName("tb_user")
public class User {

    /**
     * 用户ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名（唯一）
     */
    private String username;

    /**
     * 密码（md5 加密存储）
     */
    private String password;

    /**
     * 用户创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 最后一次更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 最近一次登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 用户状态（1：启用，2：停用）
     */
    private Integer status;
}
