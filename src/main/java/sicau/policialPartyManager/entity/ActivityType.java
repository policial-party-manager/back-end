package sicau.policialPartyManager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 活动类型实体
 * 对应数据库表：tb_activity_type
 *
 * @author sicau
 */
@Data
@TableName("tb_activity_type")
public class ActivityType {

    /**
     * 类型ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 类型名称
     */
    private String name;

    /**
     * 是否需要签到（1：需要，0：不需要）
     */
    private Integer needSign;

    /**
     * 是否计入档案（1：计入，0：不计入）
     */
    private Integer countToFile;
}
