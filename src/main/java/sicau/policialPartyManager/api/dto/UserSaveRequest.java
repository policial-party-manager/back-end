package sicau.policialPartyManager.api.dto;

import lombok.Data;

/**
 * 用户管理-新增/编辑请求
 */
@Data
public class UserSaveRequest {

    /** 用户名（登录账号），新增时必填 */
    private String username;

    /** 密码；新增时不填默认 123456（BCrypt 加密存储） */
    private String password;

    /** 姓名，必填 */
    private String realName;

    /** 学号，必填 */
    private String studentId;

    private String phone;

    private String email;

    private String identityCardNumber;

    /** 所属支部 id（可空） */
    private Long branchId;

    /** 角色 id；不填默认“普通成员”角色 */
    private Long roleId;
}
