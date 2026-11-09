package sicau.policialPartyManager.api.controller;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 契约对账：openapi.yaml 的 path 集合必须与代码里的真实映射集合完全相等（#68）。
 * <p>README「API 版本约定」第 2 条：openapi.yaml 是唯一对外契约，改接口必须同步它。
 * 这条测试把「同步」从口头约定变成会失败的断言——两边任何一侧多出或少一条都会红。
 * <p>先例：{@code ControllerAuthContractTest}、{@code DeprecatedEndpointContractTest}（同属反射契约层）。
 * <p>实现要点：必须把**无参**的 {@code @PostMapping} 也算进类级路径，
 * 否则会把 {@code /api/v4/uploads} 这类端点误判成幽灵端点。
 */
class OpenApiContractReconciliationTest {

    private static final String CONTROLLER_DIR =
            "src/main/java/sicau/policialPartyManager/api/controller";
    private static final String OPENAPI = "openapi.yaml";
    private static final String[] VERBS = {"Get", "Post", "Put", "Delete", "Patch"};

    @Test
    void openapiPathsMatchCodeMappingsExactly() throws IOException {
        Set<String> openapiPaths = readOpenApiPaths();
        Set<String> codePaths = readCodePaths();

        assertTrue(!openapiPaths.isEmpty(), "openapi.yaml 没解析出任何 path，测试本身失效");

        Set<String> declaredButMissing = new LinkedHashSet<>(openapiPaths);
        declaredButMissing.removeAll(codePaths);

        Set<String> codedButUndeclared = new LinkedHashSet<>(codePaths);
        codedButUndeclared.removeAll(openapiPaths);

        assertTrue(declaredButMissing.isEmpty(),
                "openapi.yaml 声明了代码里不存在的端点（幽灵端点）：" + declaredButMissing);
        assertTrue(codedButUndeclared.isEmpty(),
                "代码里有端点未写进 openapi.yaml（契约漂移）：" + codedButUndeclared);
    }

    /** 读 openapi.yaml 里 paths 段的每个 path（两空格缩进的键）。 */
    private Set<String> readOpenApiPaths() throws IOException {
        String text = Files.readString(Paths.get(OPENAPI), StandardCharsets.UTF_8);
        Set<String> paths = new LinkedHashSet<>();
        Matcher m = Pattern.compile("(?m)^  (/[^:]+):").matcher(text);
        while (m.find()) {
            paths.add(m.group(1));
        }
        return paths;
    }

    /** 把每个控制器的类级 @RequestMapping 与方法级 @XxxMapping 组合成完整路径。 */
    private Set<String> readCodePaths() throws IOException {
        Set<String> paths = new LinkedHashSet<>();
        try (Stream<Path> files = Files.list(Paths.get(CONTROLLER_DIR))) {
            List<Path> javaFiles = files
                    .filter(p -> p.toString().endsWith(".java"))
                    .collect(Collectors.toList());
            for (Path file : javaFiles) {
                paths.addAll(pathsOf(Files.readString(file, StandardCharsets.UTF_8)));
            }
        }
        return paths;
    }

    private Set<String> pathsOf(String source) {
        Set<String> out = new LinkedHashSet<>();
        Matcher baseMatcher = Pattern.compile("@RequestMapping\\(\"([^\"]+)\"\\)").matcher(source);
        String base = baseMatcher.find() ? baseMatcher.group(1) : "";

        for (String verb : VERBS) {
            // 同时匹配 @GetMapping 与 @GetMapping("/x") 两种形态
            Matcher m = Pattern.compile("@()" + verb + "Mapping\\b(\\s*\\(([^)]*)\\))?").matcher(source);
            while (m.find()) {
                String args = m.group(3) == null ? "" : m.group(3);
                Matcher str = Pattern.compile("\"([^\"]*)\"").matcher(args);
                String sub = str.find() ? str.group(1) : "";
                out.add(sub.isEmpty() ? base : base + sub);
            }
        }
        return out;
    }
}
