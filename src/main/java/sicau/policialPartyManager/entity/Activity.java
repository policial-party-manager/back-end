package sicau.policialPartyManager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动表实体
 * 对应数据库表：tb_activity
 *
 * @author sicau
 */
@Data
@TableName("tb_activity")
public class Activity {

    /**
     * 活动ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 支部id（关联 tb_branch.id）
     */
    private Long branchId;

    /**
     * 标题
     */
    private String title;

    /**
     * 活动简介
     */
    private String description;

    /**
     * 活动类型（关联 tb_activity_type.id）
     */
    private Integer type;

    /**
     * 封面图
     * file: 开头为本地图片
     * url: 开头为网络图片
     */
    private String cover;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更改时间
     */
    private LocalDateTime updateTime;
}
