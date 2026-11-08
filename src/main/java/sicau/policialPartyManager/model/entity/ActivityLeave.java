package sicau.policialPartyManager.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动请假实体
 * 对应数据库表：tb_activity_leave
 * <p>请假是「已批准的预期」，与 {@code tb_sign_record}（已发生的事实）生命周期不同，
 * 因此不塞进签到类型字段。
 */
@Data
@TableName("tb_activity_leave")
public class ActivityLeave {

    /** 主键ID（自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 活动id（关联 tb_activity.id） */
    private Long activityId;

    /** 请假人id（关联 tb_user.id） */
    private Long userId;

    /** 请假理由 */
    private String reason;

    /** 录入人id（关联 tb_user.id） */
    private Long operatorId;

    /** 状态（1 生效 / 0 已撤销） */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
