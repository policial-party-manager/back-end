package sicau.policialPartyManager.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 支委修改成员联系方式的请求（#70）。
 * <p>只允许改联系方式与备注：手机号 / 邮箱 / 备注。
 * 学号、姓名、学院、所属支部、政治身份属于权威字段，不在此入口的可改范围。
 * <p>部分更新语义：字段为 {@code null} 表示不动；空串表示清空。
 */
@Data
public class MemberProfileUpdateRequest {

    @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 25, message = "邮箱长度不能超过 25")
    private String email;

    @Size(max = 500, message = "备注长度不能超过 500")
    private String remark;
}
