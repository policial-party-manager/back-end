package sicau.policialPartyManager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户详情实体
 * 对应数据库表：tb_user_detail
 *
 * @author sicau
 */
@Data
@TableName("tb_user_detail")
public class UserDetail {

    /**
     * 用户id（主键，关联 tb_user.id）
     */
    @TableId(type = IdType.AUTO)
    private Long userId;

    /**
     * 党支部id
     */
    private Long branchId;

    /**
     * 学生id
     */
    private String studentId;

    /**
     * 手机号（唯一）
     */
    private String phone;

    /**
     * 学生姓名
     */
    private String name;

    /**
     * 邮箱（唯一）
     */
    private String email;

    /**
     * 身份证号
     */
    private String identityCardNumber;
}
