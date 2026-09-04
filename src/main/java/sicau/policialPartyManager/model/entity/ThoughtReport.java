package sicau.policialPartyManager.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 思想汇报表实体
 * 对应数据库表：tb_thought_report
 *
 * @author sicau
 */
@Data
@TableName("tb_thought_report")
public class ThoughtReport {

    /**
     * 主键ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户id（关联 tb_user.id）
     */
    private Long userId;

    /**
     * 审阅人（关联 tb_user.id）
     */
    private Long reviewerId;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 附件地址
     */
    private String file;

    /**
     * 审阅状态
     */
    private Integer status;

    /**
     * 审阅建议
     */
    private String reviewRemark;

    /**
     * 提交时间
     */
    private LocalDateTime submitTime;

    /**
     * 审阅时间
     */
    private LocalDateTime reviewTime;
}
