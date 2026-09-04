package sicau.policialPartyManager.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 重置密码请求
 */
@Data
public class ResetPasswordRequest {

    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{6,16}$", message = "新密码只能包含字母、数字和下划线，且长度在6到16之间")
    private String password;
}
