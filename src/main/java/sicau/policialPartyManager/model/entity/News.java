package sicau.policialPartyManager.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 新闻实体
 * 对应数据库表：tb_news
 *
 * @author sicau
 */
@Data
@TableName("tb_news")
public class News {

    /**
     * 新闻ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 作者（关联 tb_user.id）
     */
    private Long author;

    /**
     * 标题
     */
    private String title;

    /**
     * 封面
     */
    private String cover;

    /**
     * 类型
     */
    private String type;

    /**
     * 状态（1、2）
     */
    private Integer status;

    /**
     * 正文
     */
    private String content;

    /**
     * 浏览量
     */
    private Integer viewCount;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    private LocalDateTime updateTime;
}
