package sicau.policialPartyManager.api.dto;

/**
 * Bean Validation 校验分组标记。
 * <p>
 * 同一 DTO 被不同端点复用时，用分组区分“该端点必须提供哪些字段”，
 * 配合控制器参数上的 {@code @Validated(分组.class)} 使用。
 */
public final class ValidationGroups {

    private ValidationGroups() {
    }

    /** 新增场景（必填字段校验） */
    public interface Create {
    }

    /** 用户名密码登录 */
    public interface UserPass {
    }

    /** 邮箱验证码登录 */
    public interface EmailLogin {
    }

    /** 手机号验证码登录 */
    public interface PhoneLogin {
    }

    /** 携带 refresh token 的操作（刷新/退出） */
    public interface Token {
    }
}
