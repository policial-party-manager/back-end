package sicau.policialPartyManager.service;

import sicau.policialPartyManager.api.dto.LoginRequest;
import sicau.policialPartyManager.api.dto.LoginResponse;
import sicau.policialPartyManager.api.dto.MenuVo;

import java.util.List;

public interface AuthService {

    /** 用户名 + 密码登录 */
    LoginResponse loginUsernamePassword(LoginRequest request);

    /** 邮箱 + 验证码登录 */
    LoginResponse loginEmail(LoginRequest request);

    /** 手机号 + 验证码登录（短信通道暂未接入，结构已预留） */
    LoginResponse loginPhone(LoginRequest request);

    /** 发送验证码到邮箱或手机号（手机号通道留白） */
    String verifyCode(LoginRequest request);

    /** 刷新令牌，换取新的 access token / refresh token */
    LoginResponse refresh(LoginRequest request);

    /** 退出登录：吊销 refresh token（access token 到期自然失效） */
    void logout(LoginRequest request);

    /** 获取用户角色编码（super_admin / branch_admin / student） */
    String getUserRole(Long userId);
}
