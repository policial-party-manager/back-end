package sicau.policialPartyManager.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动详情实体
 * 对应数据库表：tb_activity_detail
 *
 * @author sicau
 */
@Data
@TableName("tb_activity_detail")
public class ActivityDetail {

    /**
     * 活动id（主键，关联 tb_activity.id）
     */
    @TableId(type = IdType.AUTO)
    private Long activityId;

    /**
     * 创建人id（关联 tb_user.id）
     */
    private Long creatorId;

    /**
     * 地点（polygon 空间类型，为 null 则为线上活动）
     * 以 WKT 格式字符串存储
     */
    private String location;

    /**
     * 地址（文字说明）
     */
    private String locationDescription;

    /**
     * 签到开始时间
     */
    private LocalDateTime signStart;

    /**
     * 签到结束时间
     */
    private LocalDateTime signEnd;

    /**
     * 人数上限（为 null 则为无人数限制）
     */
    private Integer maxParticipants;

    /**
     * 活动状态（0、1、2）
     */
    private Integer status;
}
