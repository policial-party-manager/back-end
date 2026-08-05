package sicau.policialPartyManager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 学习记录实体
 * 对应数据库表：tb_study_record
 *
 * @author sicau
 */
@Data
@TableName("tb_study_record")
public class StudyRecord {

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
     * 课程id（关联 tb_study.id）
     */
    private Long courseId;

    /**
     * 学时
     */
    private LocalTime studyTime;

    /**
     * 成绩
     */
    private BigDecimal score;

    /**
     * 证书链接
     */
    private String url;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
