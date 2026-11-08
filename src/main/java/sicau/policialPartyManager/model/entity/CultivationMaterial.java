package sicau.policialPartyManager.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 培养材料实体
 * 对应数据库表：tb_cultivation_material
 * <p>电子培养档案的素材：入党申请书、政审材料、培训证书等。
 */
@Data
@TableName("tb_cultivation_material")
public class CultivationMaterial {

    /** 主键ID（自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 成员id（关联 tb_user.id） */
    private Long userId;

    /** 材料类型（入党申请书/思想汇报/政审材料/培训证书/其他） */
    private String materialType;

    /** 文件地址（file: 前缀相对路径） */
    private String file;

    /** 原始文件名 */
    private String fileName;

    /** 上传人id（关联 tb_user.id） */
    private Long uploaderId;

    /** 备注 */
    private String remark;

    /** 状态（1 正常 / 0 已删除） */
    private Integer status;

    private LocalDateTime createTime;
}
