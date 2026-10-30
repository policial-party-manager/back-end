package sicau.policialPartyManager.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import sicau.policialPartyManager.repository.handler.WktPointTypeHandler;

import java.time.LocalDateTime;

/**
 * 签到记录实体
 * 对应数据库表：tb_sign_record
 *
 * @author sicau
 */
@Data
@TableName(value = "tb_sign_record", autoResultMap = true)
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
     * 签到类型（sign=本人签到，makeup=补签）
     */
    private String signType;

    /**
     * 签到设备（web/app/mini 等，可空）
     */
    private String device;

    /**
     * 地点（point 空间类型，以 WKT 字符串读写，可空）
     */
    @TableField(typeHandler = WktPointTypeHandler.class)
    private String location;

    /**
     * 备注（可空）
     */
    private String remark;
}
