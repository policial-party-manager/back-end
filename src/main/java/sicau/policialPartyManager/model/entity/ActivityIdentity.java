package sicau.policialPartyManager.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 活动目标身份实体
 * 对应数据库表：tb_activity_identity
 *
 * @author sicau
 */
@Data
@TableName("tb_activity_identity")
public class ActivityIdentity {

    /**
     * 主键ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 活动id（关联 tb_activity.id）
     */
    private Long activityId;

    /**
     * 目标身份（关联 tb_identity.id）
     */
    private Long targetIdentity;

    /**
     * 年级
     */
    private Integer grade;

    /**
     * 学院
     */
    private String college;
}
