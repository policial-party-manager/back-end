# ADR-0001: spec #68 的三处有意实现偏离

- 状态：已接受
- 日期：2026-11-09
- 上下文：spec [#68](https://github.com/policial-party-manager/back-end/issues/68) 的实现与收口

## 背景

spec #68 在「Implementation Decisions」里写下了几条具体做法。实施过程中有三处没有照做，但它们都不是疏忽，而是有具体理由的取舍。当时只把它们记在 issue 的「遗留清单」里，而 issue 会关闭、会沉底——所以移到 ADR 里长期保留。

## 决定

### 1. 培养联系人端点不放在 `/api/v4/admin/users/*`，放在 `/api/v4/members/{userId}/*`

**spec 原文**：「端点归属 `/api/v4/admin/users/*`（成员域），不新开前缀」

**实际做法**：走 `/api/v4/members/{userId}/mentors`。

**理由**：`/admin` 在本仓的既有语义是「超管专属」（`AdminController` 类级 `hasRole('SUPER_ADMIN')`）。而培养联系人的指派**正是支委的核心日常工作**——把支委的常驻功能挂进 `/admin` 前缀，会让路径名持续撒谎。`/api/v4/members/*` 与「支委可用的成员域」语义一致。

**代价**：与 spec 文字不符；若未来按 spec 字面迁移，要动 11 个集成点。

### 2. 支委权限用「独立前缀 + service 层守卫」，不用方法级 `@PreAuthorize`

**spec 原文**：「类级 `@PreAuthorize` 下沉到方法级，逐项对齐文档权限矩阵」

**实际做法**：保留 `AdminController` 的类级 `SUPER_ADMIN`；支委能力另开 `/api/v4/members/*`、`/api/v4/activities/*` 等端点；范围内判定集中在 `BranchScopeService` / `ActivityScopeGuard`。

**理由**：方法级注解的失败模式是「漏写一个就默认放行」——安全依赖每一个方法都被正确标注，加新方法时最容易忘。集中式守卫的失败模式是「漏加一次调用就默认拒绝」——错的方向是安全的一侧。两者都用反射测试兜底，但后者兜得更稳。

**代价**：与 spec 文字不符；权限矩阵不是一眼能从注解上看出来的，需要读 service。

### 3. `BatchResult` 与 `ImportResult` 保持两套，不合并成一套「部分成功」契约

**spec 原文**：「复用 Excel 导入已定型的 `ImportResult` 结构，不造第二套『部分成功』契约」

**实际做法**：两套并存。

**理由**：两者的定位键不同且都不可替代——批量操作按**业务 id**（`{id, reason}`），Excel 导入按**行号**（`{row, reason}`）。合并必然要牺牲一个：要么前端算不出「第几行错了」，要么批量失败无法定位到具体成员。

**代价**：前端要处理两种结果形状。已写入 `GLOSSARY.md` 的「批量结果 / 导入结果」词条，明确按定位键区分。

## 为什么不是「先照着 spec 改，将来再说」

三处里有两处（1 和 2）改回去会**降低**系统质量：路径名撒谎、安全边界变脆。第三处改回去会丢信息。spec 是手段不是目的；当 spec 的具体做法与它的目标（「让支部真正能自己运转」）冲突时，应当记录偏离而不是机械执行。

## 后果

- 后续接手的人读 spec 会发现这三处不一致——本 ADR 就是答案。
- 若将来要迁回 spec 字面做法，代价写在每条的「代价」里。
- `GLOSSARY.md` 收录了「批量结果 / 导入结果」的区分口径，命名输出按此执行。

## 相关

- spec: #68
- 词表：`GLOSSARY.md`
- 实施分支：`feat/spec-68-backlog`
