package sicau.policialPartyManager.api.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 类级校验：邮箱与手机号至少提供其一（用于发送验证码等场景）。
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EmailOrPhoneRequiredValidator.class)
public @interface EmailOrPhoneRequired {

    String message() default "请提供邮箱或手机号";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
