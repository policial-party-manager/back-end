package sicau.policialPartyManager.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 废弃状态契约（#51）。
 * <p>
 * {@code /api/v2/branch} 已进入「标废弃 → 观察一个迭代 → 删除」的流程，本测试锁两件事：
 * <ol>
 *   <li>该前缀下每个接口必须同时带 Java {@code @Deprecated} 与
 *       {@code @Operation(deprecated = true)}——漏一个就会让 Knife4j/openapi 与代码脱节；</li>
 *   <li>除该前缀外不得有任何接口被标废弃——防止误伤 v4 主 surface。</li>
 * </ol>
 * 将来删除 BranchController 后本测试仍应通过（届时匹配集合为空，自然成立）。
 */
class DeprecatedEndpointContractTest {

    private static final String BASE_PACKAGE = "sicau.policialPartyManager.api.controller";
    private static final String DEPRECATED_PREFIX = "/api/v2/branch";

    @SuppressWarnings("unchecked")
    private static final Class<? extends Annotation>[] HTTP_METHOD_ANNOTATIONS = new Class[]{
            GetMapping.class, PostMapping.class, PutMapping.class, DeleteMapping.class, PatchMapping.class,
            RequestMapping.class};

    @Test
    void everyDeprecatedPrefixEndpointCarriesBothAnnotations() throws Exception {
        List<String> problems = new ArrayList<>();
        int checked = 0;
        for (Class<?> controller : scanControllers()) {
            if (!DEPRECATED_PREFIX.equals(classPrefix(controller))) {
                continue;
            }
            for (Method method : handlers(controller)) {
                checked++;
                if (!method.isAnnotationPresent(Deprecated.class)) {
                    problems.add(controller.getSimpleName() + "#" + method.getName() + " 缺少 @Deprecated");
                }
                Operation operation = AnnotatedElementUtils.findMergedAnnotation(method, Operation.class);
                if (operation == null || !operation.deprecated()) {
                    problems.add(controller.getSimpleName() + "#" + method.getName()
                            + " 的 @Operation 未标 deprecated = true");
                }
            }
        }
        // 前缀还在的时候，至少应扫到接口；删掉控制器后允许为 0
        assertTrue(checked == 0 || problems.isEmpty(), () ->
                DEPRECATED_PREFIX + " 下的接口废弃标记不完整：\n - " + String.join("\n - ", problems));
        assertTrue(problems.isEmpty(), () ->
                "废弃标记问题：\n - " + String.join("\n - ", problems));
    }

    @Test
    void noOtherEndpointIsMarkedDeprecated() throws Exception {
        List<String> problems = new ArrayList<>();
        for (Class<?> controller : scanControllers()) {
            String prefix = classPrefix(controller);
            if (DEPRECATED_PREFIX.equals(prefix)) {
                continue;
            }
            for (Method method : handlers(controller)) {
                Operation operation = AnnotatedElementUtils.findMergedAnnotation(method, Operation.class);
                if (operation != null && operation.deprecated()) {
                    problems.add((prefix == null ? "?" : prefix) + " -> "
                            + controller.getSimpleName() + "#" + method.getName() + " 不应被标废弃");
                }
            }
        }
        assertTrue(problems.isEmpty(), () ->
                "存在非计划内的废弃接口：\n - " + String.join("\n - ", problems));
    }

    private java.util.Set<Class<?>> scanControllers() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        java.util.Set<Class<?>> controllers = new java.util.TreeSet<>(
                java.util.Comparator.comparing(Class::getName));
        for (var definition : scanner.findCandidateComponents(BASE_PACKAGE)) {
            controllers.add(Class.forName(definition.getBeanClassName()));
        }
        return controllers;
    }

    private String classPrefix(Class<?> controller) {
        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class);
        return mapping == null || mapping.value().length == 0 ? null : mapping.value()[0];
    }

    private List<Method> handlers(Class<?> controller) {
        List<Method> methods = new ArrayList<>();
        for (Method method : controller.getDeclaredMethods()) {
            for (Class<? extends Annotation> annotation : HTTP_METHOD_ANNOTATIONS) {
                if (method.isAnnotationPresent(annotation)) {
                    methods.add(method);
                    break;
                }
            }
        }
        return methods;
    }
}
