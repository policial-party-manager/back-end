package sicau.policialPartyManager.api.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户管理-列表/详情 VO（tb_user + tb_user_detail + 角色/支部信息）
 */
@Data
public class UserVo {

    private Long id;

    /** 用户名（登录账号） */
    private String username;

    /** 姓名 */
    private String realName;

    /** 学号 */
    private String studentId;

    private String phone;

    private String email;

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

    /** 所属支部 id */
    private Long branchId;

    /** 所属支部名称 */
    private String branchName;

    /** 角色 id */
    private Long roleId;

    /** 角色编码：super_admin / branch_admin / student */
    private String role;

    /** 状态：1 启用 / 2 停用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime lastLoginTime;
}
