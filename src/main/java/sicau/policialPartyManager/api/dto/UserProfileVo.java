package sicau.policialPartyManager.api.dto;

import lombok.Data;

/**
 * 个人中心-资料响应（GET /api/v4/user/profile）。
 * <p>
 * 取代原裸 Map 返回；字段口径与前端既有消费保持一致（账号 + 成员档案 + 所属支部）。
 */
@Data
public class UserProfileVo {

    /** 用户id */
    private Long id;

    /** 登录用户名 */
    private String username;

    /** 用户状态（1 启用 / 2 停用） */
    private Integer status;

    /** 姓名 */
    private String realName;

    /** 学号 */
    private String studentId;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 身份证号 */
    private String identityCardNumber;

    /** 性别 */
    private String gender;

    /** 学院 */
    private String college;

    /** 年级 */
    private String grade;

    /** 专业 */
    private String major;

    /** 班级 */
    private String className;

    /** 紧急联系人 */
    private String contactPerson;

    /** 备注 */
    private String remark;

    /** 所属党支部id */
    private Long branchId;

    /** 所属党支部名称 */
    private String branchName;

    /** 当前政治身份 id（tb_identity.id；null=未登记，#44） */
    private Long identityId;

    /** 当前政治身份名 */
    private String identityName;
}
