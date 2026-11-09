package sicau.policialPartyManager.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 培养联系人实体
 * 对应数据库表：tb_user_mentor
 * <p>与 {@code tb_user_detail.contact_person}（紧急联系人）是两个不同概念：
 * 培养联系人是组织指派的培养责任人，成员本人不可自改。
 */
@Data
@TableName("tb_user_mentor")
public class UserMentor {

    /** 主键ID（自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 被培养人id（关联 tb_user.id） */
    private Long userId;

    /** 培养联系人id（关联 tb_user.id） */
    private Long mentorId;

    /** 状态（1 在职 / 0 已移除） */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
