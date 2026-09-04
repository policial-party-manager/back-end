package sicau.policialPartyManager.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 签到记录实体
 * 对应数据库表：tb_sign_record
 *
 * @author sicau
 */
@Data
@TableName("tb_sign_record")
public class SignRecord {

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
     * 签到时间
     */
    private LocalDateTime signTime;

    /**
     * 签到类型
     */
    private String signType;

    /**
     * 签到设备
     */
    private String device;

    /**
     * 地点（point 空间类型，以 WKT 格式字符串存储）
     */
    private String location;

    /**
     * 备注
     */
    private String remark;
}
