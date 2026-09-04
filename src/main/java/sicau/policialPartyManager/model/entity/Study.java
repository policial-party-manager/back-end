package sicau.policialPartyManager.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 课程实体
 * 对应数据库表：tb_study
 *
 * @author sicau
 */
@Data
@TableName("tb_study")
public class Study {

    /**
     * 课程ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 课程名
     */
    private String name;

    /**
     * 类型
     */
    private String type;
}
