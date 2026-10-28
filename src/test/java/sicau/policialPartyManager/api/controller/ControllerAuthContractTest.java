package sicau.policialPartyManager.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sicau.policialPartyManager.model.enums.UserRole;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 控制器鉴权契约测试。
 * <p>
 * 背景：{@code HistoryController} 曾写 {@code @PreAuthorize("hasRole('ROLE_ADMIN')")}。
 * Spring 的 {@code hasRole} 会自动补 {@code ROLE_} 前缀，于是实际匹配 {@code ROLE_ROLE_ADMIN}，
 * 而系统只有 {@code super_admin} / {@code branch_admin} / {@code student} 三个角色，
 * 该表达式永远不可能命中——这类错误编译期发现不了，只能靠契约测试兜住。
 * <p>
 * 第二个用例针对「空壳控制器」：挂了 {@code @RestController} 却没有任何请求映射，
 * 只会在路由上留下一段死代码。
 */
class ControllerAuthContractTest {

    private static final String BASE_PACKAGE = "sicau.policialPartyManager.api.controller";
    private static final Pattern HAS_ROLE = Pattern.compile("hasRole\\(\\s*'([^']+)'\\s*\\)");
    private static final Pattern HAS_ANY_ROLE = Pattern.compile("hasAnyRole\\(([^)]*)\\)");

    /** 系统实际具备的角色编码（大写形式，正好是 hasRole 的参数形态） */
    private static final Set<String> KNOWN_ROLES = Arrays.stream(UserRole.values())
            .map(role -> role.getCode().toUpperCase(Locale.ROOT))
            .collect(Collectors.toCollection(LinkedHashSet::new));

    @Test
    void everyHasRoleNamesAnExistingRole() throws Exception {
        List<String> problems = new ArrayList<>();
        for (Class<?> controller : scanControllers()) {
            for (String expression : preAuthorizeExpressions(controller)) {
                problems.addAll(checkExpression(controller, expression));
            }
        }
        assertTrue(problems.isEmpty(), () ->
                "有 @PreAuthorize 引用了不存在的角色（hasRole 会自动补 ROLE_ 前缀）：\n - "
                        + String.join("\n - ", problems)
                        + "\n现有角色：" + KNOWN_ROLES);
    }

    @Test
    void everyRestControllerExposesAtLeastOneEndpoint() throws Exception {
        List<String> empty = new ArrayList<>();
        for (Class<?> controller : scanControllers()) {
            boolean hasEndpoint = Arrays.stream(controller.getDeclaredMethods())
                    .anyMatch(method -> AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class));
            if (!hasEndpoint) {
                empty.add(controller.getSimpleName());
            }
        }
        assertTrue(empty.isEmpty(), () ->
                "以下 @RestController 没有任何请求映射，空壳控制器应删除或补上实现：" + empty);
    }

    /** 扫描 api.controller 包下所有 @RestController */
    private Set<Class<?>> scanControllers() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        Set<Class<?>> controllers = new TreeSet<>(Comparator.comparing(Class::getName));
        for (var definition : scanner.findCandidateComponents(BASE_PACKAGE)) {
            controllers.add(Class.forName(definition.getBeanClassName()));
        }
        return controllers;
    }

    /** 收集类级 + 方法级的 @PreAuthorize 表达式 */
    private List<String> preAuthorizeExpressions(Class<?> controller) {
        List<String> expressions = new ArrayList<>();
        PreAuthorize onClass = AnnotatedElementUtils.findMergedAnnotation(controller, PreAuthorize.class);
        if (onClass != null) {
            expressions.add(onClass.value());
        }
        for (Method method : controller.getDeclaredMethods()) {
            PreAuthorize onMethod = AnnotatedElementUtils.findMergedAnnotation(method, PreAuthorize.class);
            if (onMethod != null) {
                expressions.add(onMethod.value());
            }
        }
        return expressions;
    }

    private List<String> checkExpression(Class<?> controller, String expression) {
        List<String> problems = new ArrayList<>();

        Matcher single = HAS_ROLE.matcher(expression);
        while (single.find()) {
            addIfUnknown(problems, controller, expression, single.group(1));
        }

        Matcher any = HAS_ANY_ROLE.matcher(expression);
        while (any.find()) {
            for (String raw : any.group(1).split(",")) {
                addIfUnknown(problems, controller, expression, raw.trim().replace("'", ""));
            }
        }
        return problems;
    }

    private void addIfUnknown(List<String> problems, Class<?> controller, String expression, String role) {
        String normalized = role.toUpperCase(Locale.ROOT);
        if (normalized.isEmpty() || KNOWN_ROLES.contains(normalized)) {
            return;
        }
        problems.add(controller.getSimpleName() + " -> " + expression
                + "（" + role + " 不是现有角色，注意 hasRole 不要自己再写 ROLE_ 前缀）");
    }
}
