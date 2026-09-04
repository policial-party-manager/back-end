package sicau.policialPartyManager.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 党支部表实体
 * 对应数据库表：tb_branch
 *
 * @author sicau
 */
@Data
@TableName("tb_branch")
public class Branch {

    /**
     * 支部ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 支部名称
     */
    private String branchName;

    /**
     * 所属学院
     */
    private String college;

    /**
     * 支部书记用户id（关联 tb_user.id）
     */
    private Long secretaryId;

    /**
     * 支部简介
     */
    private String description;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    private LocalDateTime updateTime;

    /**
     * 是否删除（0：未删除，1：已删除）
     */
    private Integer status;
}
