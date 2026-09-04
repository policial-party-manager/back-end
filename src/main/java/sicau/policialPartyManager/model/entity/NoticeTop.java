package sicau.policialPartyManager.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 公告置顶消息实体
 * 对应数据库表：tb_notice_top
 *
 * @author sicau
 */
@Data
@TableName("tb_notice_top")
public class NoticeTop {

    /**
     * 主键ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 公告id（关联 tb_notice.id）
     */
    private Long noticeId;

    /**
     * 给哪些用户置顶（0 为所有用户，关联 tb_role.id）
     */
    private Long roleId;
}
