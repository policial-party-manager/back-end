# 数据库 ER 图

党建云平台后端共 **21 张表**，按四个域组织。对应 DDL 在 [`sql/tb_*.sql`](./)，一键建库见 [`sql/databases.sql`](./databases.sql)。

> 设计基线：2026-08-03 收敛版 ER（讨论见历史 issue「关于sql」）。2026-07 的初版表清单（`tb_member` / `tb_permission` / `tb_resource` / `tb_activity_member`）已被本版取代 —— `tb_member` → `tb_user_detail`，`tb_activity_member` → `tb_activity_identity`，`tb_permission` → `tb_role_permission`，`tb_resource` 未采用。

---

## 总览

```mermaid
erDiagram
    tb_user ||--o| tb_user_detail : "1:1 用户详情"
    tb_user ||--o{ tb_user_role : "N:M 角色"
    tb_role ||--o{ tb_user_role : ""
    tb_role ||--o{ tb_role_permission : "N:M 权限"
    tb_role ||--o{ tb_identity : "身份对应角色"

    tb_user ||--o{ tb_branch : "书记"
    tb_branch ||--o{ tb_user_detail : "支部成员"

    tb_identity ||--o{ tb_identity_change : "来源身份"
    tb_identity ||--o{ tb_identity_change : "目标身份"
    tb_user ||--o{ tb_identity_change : "被变更用户"
    tb_identity ||--o{ tb_user_detail : "当前身份"

    tb_activity_type ||--o{ tb_activity : "活动类型"
    tb_branch ||--o{ tb_activity : "主办支部"
    tb_activity ||--o| tb_activity_detail : "1:1 活动详情"
    tb_activity ||--o{ tb_activity_identity : "目标身份"
    tb_identity ||--o{ tb_activity_identity : ""
    tb_activity ||--o{ tb_sign_record : "签到"
    tb_user ||--o{ tb_sign_record : ""
    tb_activity ||--o{ tb_activity_signup : "报名"
    tb_user ||--o{ tb_activity_signup : ""

    tb_study ||--o{ tb_study_record : "课程学习记录"
    tb_user ||--o{ tb_study_record : ""

    tb_user ||--o{ tb_thought_report : "提交人"
    tb_user ||--o{ tb_thought_report : "审阅人"

    tb_user ||--o{ tb_news : "作者"
    tb_user ||--o{ tb_notice : "发布者"
    tb_notice ||--o{ tb_notice_top : "置顶"
    tb_role ||--o{ tb_notice_top : "置顶范围(0=全体)"

    tb_user ||--o{ tb_operation : "操作者"
```

---

## 四域拆分

| 域 | 表 |
| --- | --- |
| **用户权限** | `tb_user`、`tb_user_detail`、`tb_user_role`、`tb_role`、`tb_role_permission` |
| **组织与身份** | `tb_branch`、`tb_identity`、`tb_identity_change`（`tb_user_detail.identity_id` 存当前身份） |
| **党建业务** | `tb_activity`、`tb_activity_detail`、`tb_activity_identity`、`tb_activity_signup`、`tb_activity_type`、`tb_sign_record`、`tb_study`、`tb_study_record`、`tb_thought_report` |
| **内容与日志** | `tb_news`、`tb_notice`、`tb_notice_top`、`tb_operation` |

`tb_activity_type` 与 `tb_study` 是纯字典表，无外键依赖。

---

## 关键字段与约束

### `tb_user` / `tb_user_detail`

- 账号与档案拆表：`tb_user` 只存登录凭据与状态（`status`：1 启用 / 2 停用）；`tb_user_detail` 以 `user_id` 作主键，1:1
- `tb_user_detail` 上 `email`、`phone`、`student_id` 各有唯一索引
- 政治身份「当前值」存 `tb_user_detail.identity_id`（#44 新增，NULL=未登记）；变更历史与审批单存 `tb_identity_change`（两阶段：1 待审批 / 2 已生效 / 3 驳回），生效时同事务写回当前身份；**身份变更永不联动登录角色**

### `tb_role` / `tb_role_permission`

- `role_name` 受 CHECK 约束：必须以 `ROLE_` 开头（`chk_role_code_prefix`）
- 权限为「角色 → 权限名」的字符串映射（`permission_name`），不是独立的权限主数据表
- 曾经的 `tb_user_permission` 是这两张表的错误重复（列名还拼错成 `permission_nmae`），已于 2026-10 删除，不要再恢复

### `tb_branch`

- `secretary_id` 指向 `tb_user.id`，书记是用户而非单独实体
- `status`：`1` 正常 / `0` 已删除（软删除），DDL 默认值同为 `1`。全项目统一约定见 README「软删除」节

### `tb_study` / `tb_study_record`

- 课程字典：`name` 全表唯一（应用层 + `tb_study_name_uindex`）；`type` 自由文本（varchar(20)），不预设枚举
- 学习记录：**一人一课一行**（`unique(user_id, course_id)`），重复录入即覆盖更新
- `score` decimal(5,2)（0~100 支持两位小数）；`user_id` NOT NULL；`study_time` 为 time 类型（汇总按分钟计）

### `tb_activity` / `tb_activity_detail`

- 1:1 拆分为主表 + 详情表：主表存标题/封面/时间/类型/支部，详情表存地点、人数上限、签到窗、状态
- `branch_id` 可空 → 空表示校级活动
- `type` → `tb_activity_type`，用于「是否需要签到 / 是否计入档案」这类行为开关

### `tb_sign_record`

- `activity_id` + `user_id` 构成业务上的签到关系
- `location` 用 MySQL `point` 类型（WKT），`device`/`sign_type` 记录渠道

### `tb_activity_signup`

- 一行 = 一个用户对某活动的一次有效报名；取消即删除该行（名额随之释放）
- `unique(activity_id, user_id)` 挡重复报名；`signup_time` 默认当前时间
- 报名身份维度已接通（#61）：行的 `target_identity` 与用户 `tb_user_detail.identity_id` 相等才算命中；未登记身份报名限定活动时给出「联系支部管理员提交身份变更」提示

### `tb_identity` / `tb_identity_change`（#44）

- `tb_identity`：6 级政治身份字典（level 1~6），迁移播种、`identity_name` 唯一、超管可维护；`role_id` 可空且**仅展示映射，不参与鉴权**
- `tb_identity_change`：两阶段单据——`from_identity` 发起时快照（首次登记 NULL）、`status` CHECK(1,2,3)、审批人/时间/意见；仅超管审批，通过才写回 `identity_id`
- 同一用户同时只允许一张待审批单

### `tb_notice` / `tb_notice_top`

- 公告展示期由 `start_time` / `end_time` 控制（可空不限）
- `status`：1 正常 / 2 已删除 / 3 延期展示
- 置顶按角色粒度：`tb_notice_top.role_id = 0` 表示对全体置顶
### `tb_activity_signup`

- 一行代表一个用户对某活动的一次有效报名；取消即删除该行（名额随之释放）
- `unique(activity_id, user_id)` 挡重复报名；`signup_time` 默认当前时间
- **身份维度已接通（#61）**：`tb_activity_identity.target_identity` 与 `tb_user_detail.identity_id` 相等比较；未登记身份报名限定活动 → 400 可执行提示；行未设 target_identity 时维持年级/学院语义


---

## 改表时的做法

1. 改 `sql/tb_<table>.sql`（新库从零建表用），并对已有环境补一个 `sql/alter_*.sql` 幂等迁移脚本
2. 跑 `./sql/schema-verify.sh`：它会在临时库里按依赖顺序建一遍全部表，任何语法/编码/顺序问题都会立刻变红
3. 同步更新本文件的 Mermaid 图与「关键字段」段，避免文档再次落后于 DDL

历史脚本都是 EF/Core 导出风格（`create table ... ` 小写、`constraint` 内联），新脚本请沿用同一风格。
