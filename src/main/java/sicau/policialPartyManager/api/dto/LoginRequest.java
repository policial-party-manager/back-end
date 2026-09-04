package sicau.policialPartyManager.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 认证通用请求体。不同接口只使用其中部分字段，必填性通过校验分组按端点控制：
 * <ul>
 *   <li>用户名密码登录：{@code username} + {@code password}（UserPass 组）</li>
 *   <li>邮箱验证码登录：{@code email} + {@code verifyCode}（EmailLogin 组）</li>
 *   <li>手机号验证码登录：{@code phone} + {@code verifyCode}（PhoneLogin 组）</li>
 *   <li>发送验证码：{@code email} 或 {@code phone} 至少其一（类级校验）</li>
 *   <li>刷新 token / 退出登录：{@code refreshToken}（Token 组）</li>
 * </ul>
 */
@EmailOrPhoneRequired
@Data
public class LoginRequest {

    private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final String PHONE_REGEX = "^1\\d{10}$";

    /** 用户名（用户名密码登录） */
    @NotBlank(groups = ValidationGroups.UserPass.class, message = "用户名不能为空")
    private String username;

    /** 密码（用户名密码登录） */
    @NotBlank(groups = ValidationGroups.UserPass.class, message = "密码不能为空")
    private String password;

    /** 邮箱（邮箱验证码登录 / 发送验证码） */
    @Pattern(regexp = "^$|" + EMAIL_REGEX, message = "邮箱格式不正确")
    @NotBlank(groups = ValidationGroups.EmailLogin.class, message = "邮箱不能为空")
    @Pattern(groups = ValidationGroups.EmailLogin.class, regexp = EMAIL_REGEX, message = "邮箱格式不正确")
    private String email;

    /** 手机号（手机号验证码登录 / 发送验证码，短信通道暂未接入） */
    @Pattern(regexp = "^$|" + PHONE_REGEX, message = "手机号格式不正确")
    @NotBlank(groups = ValidationGroups.PhoneLogin.class, message = "手机号不能为空")
    @Pattern(groups = ValidationGroups.PhoneLogin.class, regexp = PHONE_REGEX, message = "手机号格式不正确")
    private String phone;

    /** 验证码（邮箱/手机号验证码登录） */
    @NotBlank(groups = {ValidationGroups.EmailLogin.class, ValidationGroups.PhoneLogin.class},
            message = "验证码不能为空")
    private String verifyCode;

    /** 刷新令牌（刷新 / 退出登录） */
    @NotBlank(groups = ValidationGroups.Token.class, message = "refreshToken 不能为空")
    private String refreshToken;
}
