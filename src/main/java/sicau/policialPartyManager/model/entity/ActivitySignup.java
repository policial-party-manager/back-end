package sicau.policialPartyManager.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动报名实体
 * 对应数据库表：tb_activity_signup
 * <p>
 * 一行 = 一个用户对某活动的一次有效报名；取消即删除该行（名额随之释放）。
 * unique(activity_id, user_id) 挡重复报名。
 *
 * @author sicau
 */
@Data
@TableName("tb_activity_signup")
public class ActivitySignup {

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
     * 用户id（关联 tb_user.id）
     */
    private Long userId;

    /**
     * 报名时间（数据库默认 CURRENT_TIMESTAMP）
     */
    private LocalDateTime signupTime;
}
