# 双轨 API 与前端对接契约

> 对应 issue：research: 双轨 API 与前端对接契约（v1/v2/v4 + openapi）
> 调研日期：2026-10-28 ｜ 方法：仓内一手来源（Controller 注解、`openapi.yaml`、`SecurityConfig`、git 历史）

## TL;DR

1. **`openapi.yaml` 与代码 100% 同步**：53 条 path、70 个 operation，两侧完全一致，没有幽灵端点也没有漏文档。它可以直接被宣布为唯一契约。
2. **版本号是「重组目录时顺手加的」，没有版本策略**：`/api/v1` 与 `/api/v4` 同时出现在同一次目录重组提交里；全仓不存在 `/api/v3`。
3. **真正的双轨只有支部域**：`/api/v2/branch`（5 个 operation）与 `/api/v4/admin/branches/*`（8 个 path）并存，且 v2 的能力严格弱于 v4。
4. **建议**：以 `openapi.yaml` 为唯一契约 → 冻结版本前缀 → 下线 `/api/v2/branch`。
   下线前必须先确认前端是否仍在调用它——**本工作区没有前端仓库，这一点无法从仓内证据判定**（见「未解疑问」）。

## 范围与方法

- 端点集合由 `src/main/java/sicau/policialparty*/api/controller/*.java` 的类级 `@RequestMapping` 与方法级映射组合而成
- 契约侧取 `openapi.yaml` 的 `paths`
- 鉴权取值来源：`config/SecurityConfig.java`（全局链路）+ 各控制器类级 `@PreAuthorize`
- **不存在**前端仓库（同级目录只有 `back-end/`），因此「前端实际调用表」无法用一手来源回答；本笔记按 issue 允许的另一条路走：**论证 openapi 可作唯一契约**，并给出下线前必须确认的事项

## 现状盘点

| 前缀 | 控制器 | path 数 | operation 数 | 鉴权 |
| --- | --- | --- | --- | --- |
| `/api/v1/auth` | `AuthController` | 8 | 8 | **免登录**（`SecurityConfig:43,56` 放行 `/api/v1/auth/**`） |
| `/api/v2/content` | `ContentController` | 7 | 7 | 仅需登录；只返回已发布内容 |
| `/api/v2/branch` | `BranchController` | 3 | 5 | `hasRole('BRANCH_ADMIN')`（`BranchController:18`） |
| `/api/v4/admin` | `AdminController` | 34 | 48 | `hasRole('SUPER_ADMIN')`（`AdminController:35`） |
| `/api/v4/user` | `UserController` | 1 | 2 | 仅需登录 |
| **合计** | | **53** | **70** | |

全局规则：除接口文档与 `/api/v1/auth/**` 外，其余请求一律要求已登录（`SecurityConfig:55-57`）。

## 发现

### 1. `openapi.yaml` 是可信的，且与代码零漂移

```
代码 path 数 = 53           openapi path 数 = 53      → 集合完全相同
代码 operation 数 = 70      openapi operation 数 = 70 → 数量相同
```

它于 `375fb6f 创建 openapi.yaml` 引入，随后在 `7b96eba 更新 openapi.yaml` 同步过一次，最近一次大改跟 SSO 接入（`8bf2631`）一起。结论：**它现在是一份忠实快照**，可以作为契约基线——但它是**手写维护**的，不会自动跟随注解变化，这是唯一的长期风险。

### 2. 版本号没有策略

- `/api/v1` 与 `/api/v4` 的首次出现都在 `2bbe81b drayee/organize-files` 这一次**目录重组**提交里
- `/api/v2` 首次出现在 `14991ab drayee/build-program`
- 全仓（代码 / 文档 / openapi）**不存在 `/api/v3`**

也就是说：三个版本号不是「对外 API 的三代」，而是不同模块在整理文件时各自取的名字。它们没有语义（不代表 breaking change），也没有迁移路径。

### 3. 真正的双轨只有支部域

内容域没有重复：`/api/v2/content` 只读、`/api/v4/admin/news|notices|activities` 负责写，职责互补。

支部域则完全重叠：

| 能力 | `/api/v2/branch` | `/api/v4/admin/branches/*` |
| --- | --- | --- |
| 列表 | `GET /list`（全量，无分页） | `GET /page`（分页 + keyword 搜索） |
| 详情 | `GET /{id}` | `GET /{id}` |
| 新增 | `POST`（直接吞实体，无校验） | `POST`（校验支部名唯一、书记必填） |
| 编辑 | `PUT /{id}`（直接吞实体） | `PUT /{id}`（部分更新） |
| 删除 | `DELETE /{id}` | `DELETE /{id}` |
| 下拉选项 | ✗ | `GET /options` |
| 导入模板 / 批量导入 | ✗ | `GET /template`、`POST /import` |
| 鉴权 | 仅 `BRANCH_ADMIN`（**超管反而 403**） | `SUPER_ADMIN` |

**v2 是 v4 的严格弱化子集**：没有分页、没有搜索、没有校验、没有导入，而且鉴权把超级管理员挡在门外（这正是 #39 记录的问题）。它的 `save()` 还曾是 #42 那个「新建支部立刻变成已删除」bug 的现场。

### 4. 由此可推出一条判断

只要支部域收敛到一套接口，#39（BranchController 角色矩阵）就从一个「要不要给超管放行」的权衡题，变成「删掉重复实现」的清理题——问题本身消失。

## 建议

| # | 建议 | 依据 |
| --- | --- | --- |
| 1 | **宣布 `openapi.yaml` 为唯一对外契约**，README 的 API 章节指向它而不是逐条重抄 | 53/53、70/70 零漂移；README 逐条重抄必然再次落后（#41 的成因） |
| 2 | **冻结版本前缀**：不新增 `/api/v5`；新端点并入其所属域的现有前缀 | 三个版本号无语义、无迁移路径，再加一个只会增加混乱 |
| 3 | **下线 `/api/v2/branch`**：先标 `@Deprecated` 并在 openapi 里注明，观察一个迭代后再移除 | v2 是 v4 的严格弱化子集，保留两套只会继续分裂 |
| 4 | 下线动作完成后再看 #39 | 支部域只剩一套接口时，#39 的角色矩阵问题自动消解 |

## 未解疑问（需要人确认，agent 无法从仓内判定）

1. **前端是否仍在调用 `/api/v2/branch`？** 本工作区没有前端仓库。这是下线前唯一的硬前提。
   - 建议动作：问前端负责人，或在网关/日志里看 `/api/v2/branch` 的访问量
2. `openapi.yaml` 由谁、用什么工具生成？如果是 Knife4j/Swagger 导出，应把导出命令写进 README，避免继续手抄。

## 证据引用

| 结论 | 来源 |
| --- | --- |
| 免登录白名单只有文档与 auth | `src/main/java/sicau/policialPartyManager/config/SecurityConfig.java:39-43,55-57` |
| admin 全域要求 SUPER_ADMIN | `src/main/java/sicau/policialPartyManager/api/controller/AdminController.java:35` |
| v2 支部只允许 BRANCH_ADMIN | `src/main/java/sicau/policialPartyManager/api/controller/BranchController.java:18` |
| v2 支部 5 个 operation、无分页无导入 | `BranchController.java:25,31,37,44,52` |
| v4 支部 8 条 path（含 options/template/import） | `AdminController.java:114,122,128,135,142,149,155,161` |
| openapi 53 path / 70 operation | `openapi.yaml`（`grep -cE '^  /'`、`grep -cE '^ +(get|post|put|delete|patch):$'`） |
| openapi 由 `375fb6f` 创建、`7b96eba` 更新 | `git log -- openapi.yaml` |
| v1/v4 首现于同一次目录重组 | `git log --reverse -S"api/v1" -- '*.java'` → `2bbe81b drayee/organize-files` |
| v2 首现于 build-program | `git log --reverse -S"api/v2" -- '*.java'` → `14991ab drayee/build-program` |
| 全仓无 `/api/v3` | `grep -rn "api/v3" --include=*.java --include=*.md --include=*.yaml .` 无输出 |
