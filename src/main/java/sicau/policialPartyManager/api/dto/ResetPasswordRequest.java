package sicau.policialPartyManager.api.dto;

import lombok.Data;

/**
 * 重置密码请求
 */
@Data
public class ResetPasswordRequest {

    /** 新密码；为空则由服务端重置为默认密码 123456 */
    private String password;
}
