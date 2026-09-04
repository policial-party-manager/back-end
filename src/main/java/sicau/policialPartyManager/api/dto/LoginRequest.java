package sicau.policialPartyManager.api.dto;

import lombok.Data;

/**
 * 认证通用请求体。
 * <p>
 * 不同接口只使用其中部分字段，各字段是否必填由服务层按接口校验：
 * <ul>
 *   <li>用户名密码登录：{@code username} + {@code password}</li>
 *   <li>邮箱验证码登录：{@code email} + {@code verifyCode}</li>
 *   <li>手机号验证码登录：{@code phone} + {@code verifyCode}</li>
 *   <li>发送验证码：{@code email} 或 {@code phone}（二选一）</li>
 *   <li>刷新 token：{@code refreshToken}</li>
 *   <li>退出登录：{@code refreshToken}</li>
 * </ul>
 */
@Data
public class LoginRequest {

    /** 用户名（用户名密码登录） */
    private String username;

    /** 密码（用户名密码登录） */
    private String password;

    /** 邮箱（邮箱验证码登录 / 发送验证码） */
    private String email;

    /** 手机号（手机号验证码登录 / 发送验证码，短信通道暂未接入） */
    private String phone;

    /** 验证码（邮箱/手机号验证码登录） */
    private String verifyCode;

    /** 刷新令牌（刷新 / 退出登录） */
    private String refreshToken;
}
