package sicau.policialPartyManager.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体
 * 对应数据库表：tb_operation
 *
 * @author sicau
 */
@Data
@TableName("tb_operation")
public class Operation {

    /**
     * 主键ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 操作者id（关联 tb_user.id）
     */
    private Long operatorId;

    /**
     * 操作ip
     */
    private String ipAddress;

    /**
     * 操作
     */
    private String operator;

    /**
     * 操作时间
     */
    private LocalDateTime time;

    /**
     * 浏览器UA
     */
    private String userAgent;

    /**
     * 结果
     */
    private String result;
}
