package sicau.policialPartyManager.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 用户管理-新增/编辑请求。
 * <p>
 * username/realName/studentId 仅在“新增(Create 组)”时必填；
 * 编辑支持部分更新（未传字段保留原值），仅做格式校验。
 */
@Data
public class UserSaveRequest {

    private static final String PASSWORD_REGEX = "^[a-zA-Z0-9_]{6,16}$";
    private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final String PHONE_REGEX = "^1\\d{10}$";

    /** 用户名（登录账号），新增时必填；编辑时不可修改（忽略） */
    @NotBlank(groups = ValidationGroups.Create.class, message = "用户名不能为空")
    private String username;

    /** 密码；新增时不填默认 123456；填写时须满足密码规则 */
    @Pattern(regexp = "^$|" + PASSWORD_REGEX, message = "密码只能包含字母、数字和下划线，且长度在6到16之间")
    private String password;

    /** 姓名 */
    @NotBlank(groups = ValidationGroups.Create.class, message = "姓名不能为空")
    private String realName;

    /** 学号 */
    @NotBlank(groups = ValidationGroups.Create.class, message = "学号不能为空")
    private String studentId;

    @Pattern(regexp = "^$|" + PHONE_REGEX, message = "手机号格式不正确")
    private String phone;

    @Pattern(regexp = "^$|" + EMAIL_REGEX, message = "邮箱格式不正确")
    private String email;

    private String identityCardNumber;

    /** 所属支部 id（可空） */
    private Long branchId;

    /** 角色 id；不填默认“普通成员”角色 */
    private Long roleId;
}
