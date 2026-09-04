package sicau.policialPartyManager.api.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

/**
 * {@link EmailOrPhoneRequired} 的实现：邮箱或手机号至少一个非空。
 */
public class EmailOrPhoneRequiredValidator implements ConstraintValidator<EmailOrPhoneRequired, LoginRequest> {

    @Override
    public boolean isValid(LoginRequest value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return StringUtils.hasText(value.getEmail()) || StringUtils.hasText(value.getPhone());
    }
}
