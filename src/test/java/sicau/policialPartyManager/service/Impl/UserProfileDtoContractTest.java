package sicau.policialPartyManager.service.Impl;

import org.junit.jupiter.api.Test;
import sicau.policialPartyManager.api.dto.UserProfileUpdateRequest;
import sicau.policialPartyManager.api.dto.UserProfileVo;
import sicau.policialPartyManager.service.UserService;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * #36 的接口形态契约：个人中心不再有裸 Map / Object。
 * <p>
 * 签名层面锁死：profile 返回 UserProfileVo；updateProfile 入参 UserProfileUpdateRequest。
 * 若有人改回 Map，本测试在编译期即失败（运行期断言防退化签名绕过）。
 */
class UserProfileDtoContractTest {

    @Test
    void profileReturnsTypedVo() throws Exception {
        Method method = UserService.class.getMethod("profile", sicau.policialPartyManager.model.records.User.class);
        assertEquals(UserProfileVo.class, method.getReturnType());
    }

    @Test
    void updateProfileTakesTypedRequest() throws Exception {
        Method method = UserService.class.getMethod("updateProfile",
                UserProfileUpdateRequest.class, sicau.policialPartyManager.model.records.User.class);
        assertEquals(void.class, method.getReturnType());
    }

    @Test
    void updateRequestHasNoMapLikeEscapeHatch() {
        // 白名单即字段集：不允许出现 Object/Map 类型字段
        for (var field : UserProfileUpdateRequest.class.getDeclaredFields()) {
            String type = field.getType().getName();
            org.junit.jupiter.api.Assertions.assertFalse(
                    type.startsWith("java.util.Map") || type.equals("java.lang.Object"),
                    "更新请求不得包含逃逸字段: " + field.getName());
        }
    }
}
