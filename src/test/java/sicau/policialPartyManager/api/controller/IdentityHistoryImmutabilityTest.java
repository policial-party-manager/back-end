package sicau.policialPartyManager.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 身份变更历史不可删（#75）。
 * <p>「不写 @DeleteMapping」只是当前事实，不是约束——任何人加一个删除映射它就失效了。
 * 这条反射测试把口头约定变成会失败的测试：
 * 扫描 IdentityChangeController 的所有方法，断言不存在任何删除语义的映射。
 * <p>同时断言该控制器不暴露「作废」入口：作废只允许运维手段执行，
 * 一旦有人加了把单据置 0 的端点，这里会红。
 */
class IdentityHistoryImmutabilityTest {

    private static final Class<?> CONTROLLER =
            sicau.policialPartyManager.api.controller.IdentityChangeController.class;

    @Test
    void noDeleteMappingAnywhere() {
        List<String> offenders = new ArrayList<>();
        for (Method m : CONTROLLER.getDeclaredMethods()) {
            if (m.getAnnotation(DeleteMapping.class) != null) {
                offenders.add(m.getName());
            }
        }
        assertTrue(offenders.isEmpty(),
                "身份变更控制器不得提供删除端点（历史不可删），违规方法：" + offenders);
    }

    @Test
    void noVoidEndpoint() {
        // 作废（status=0）只能由运维手段执行，不得有外部入口
        List<String> offenders = new ArrayList<>();
        for (Method m : CONTROLLER.getDeclaredMethods()) {
            String name = m.getName().toLowerCase();
            if (name.contains("void") || name.contains("invalidate") || name.contains("cancel")) {
                offenders.add(m.getName());
            }
        }
        assertTrue(offenders.isEmpty(),
                "身份变更控制器不得暴露作废入口，违规方法：" + offenders);
    }

    @Test
    void controllerMappingIsStable() {
        RequestMapping mapping = CONTROLLER.getAnnotation(RequestMapping.class);
        assertTrue(mapping != null, "控制器缺少 @RequestMapping");
        assertTrue(List.of(mapping.value()).contains("/api/v4/identity-changes"),
                "身份变更控制器路径变了，请同步更新本条契约测试");
    }
}
