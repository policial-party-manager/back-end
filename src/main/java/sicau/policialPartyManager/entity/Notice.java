package sicau.policialPartyManager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告表实体
 * 对应数据库表：tb_notice
 *
 * @author sicau
 */
@Data
@TableName("tb_notice")
public class Notice {

    /**
     * 公告ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容（以 file: 开头时识别为文件）
     */
    private String content;

    /**
     * 状态
     * 1：正常
     * 2：已经删除
     * 3：延期展示
     */
    private Integer status;

    /**
     * 发布者的id（0 为系统发布）
     */
    private Long publisherId;

    /**
     * 发布时间
     */
    private LocalDateTime publishTime;

    /**
     * 修改时间
     */
    private LocalDateTime updateTime;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;
}
