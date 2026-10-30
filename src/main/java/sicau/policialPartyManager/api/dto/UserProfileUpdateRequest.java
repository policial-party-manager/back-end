package sicau.policialPartyManager.api.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 个人中心-编辑请求（PUT /api/v4/user/profile）。
 * <p>
 * 白名单即下列字段；<b>部分更新</b>：未传或空值 = 不修改该字段。
 * 请求体中的未知字段被忽略（Jackson 默认行为，与编辑类端点既有策略一致）。
 * 长度校验对齐 tb_user_detail 列宽，超长在 400 拦截而不是等数据库截断/报错。
 */
@Data
public class UserProfileUpdateRequest {

    private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final String PHONE_REGEX = "^1\\d{10}$";

    /** 姓名（tb_user_detail.name varchar(12)） */
    @Size(max = 12, message = "姓名长度不能超过 12")
    private String realName;

    /** 学号（varchar(10)） */
    @Size(max = 10, message = "学号长度不能超过 10")
    private String studentId;

    /** 身份证号（varchar(18)） */
    @Size(max = 18, message = "身份证号长度不能超过 18")
    private String identityCardNumber;

    /** 手机号（可空；填写须为 11 位大陆号码） */
    @Pattern(regexp = "^$|" + PHONE_REGEX, message = "手机号格式不正确")
    private String phone;

    /** 邮箱（可空；填写须为合法邮箱） */
    @Pattern(regexp = "^$|" + EMAIL_REGEX, message = "邮箱格式不正确")
    @Size(max = 25, message = "邮箱长度不能超过 25")
    private String email;

    /** 性别（varchar(10)） */
    @Size(max = 10, message = "性别长度不能超过 10")
    private String gender;

    /** 学院（varchar(50)） */
    @Size(max = 50, message = "学院长度不能超过 50")
    private String college;

    /** 年级（varchar(10)） */
    @Size(max = 10, message = "年级长度不能超过 10")
    private String grade;

    /** 专业（varchar(50)） */
    @Size(max = 50, message = "专业长度不能超过 50")
    private String major;

    /** 班级（varchar(50)） */
    @Size(max = 50, message = "班级长度不能超过 50")
    private String className;

    /** 紧急联系人（varchar(50)） */
    @Size(max = 50, message = "紧急联系人长度不能超过 50")
    private String contactPerson;

    /** 备注（varchar(500)） */
    @Size(max = 500, message = "备注长度不能超过 500")
    private String remark;
}
