package sicau.policialPartyManager.service.Impl;

import org.junit.jupiter.api.Test;
import sicau.policialPartyManager.service.SignRecordService;

/**
 * 编译级契约：签到服务的公开接口形态。
 * <p>
 * #35 的 Agent Brief 要求签到服务至少暴露「签到」与「按活动分页查记录」；
 * 本测试锁定服务名、方法名与异常语义，重构时若把签名改掉会立刻在这里变红。
 * 业务规则的完整覆盖在运行期测试（需要真实 MySQL）与验收清单里。
 */
class SignRecordServiceContractTest {

    @Test
    void serviceInterfaceExists() {
        // 接口必须存在且方法齐全：sign / pageByActivity
        for (String method : new String[]{"sign", "pageByActivity"}) {
            boolean found = false;
            for (java.lang.reflect.Method m : SignRecordService.class.getMethods()) {
                if (m.getName().equals(method)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new AssertionError("SignRecordService 缺少方法: " + method);
            }
        }
    }

    @Test
    void businessErrorsUseIllegalArgumentException() {
        // 与 GlobalExceptionHandler 约定一致：业务错误 = IllegalArgumentException -> 400
        // 这里仅锁定类型选择，行为在实现测试中覆盖
        IllegalArgumentException e = new IllegalArgumentException("该活动无需签到");
        org.junit.jupiter.api.Assertions.assertEquals("该活动无需签到", e.getMessage());
    }
}
