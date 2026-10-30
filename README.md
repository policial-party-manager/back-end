# 党建云平台 · Policial Party Manager

高校党建信息管理后端服务。基于 **Spring Boot 3.2 + MyBatis-Plus + Spring Security + JWT**，提供认证（含学校 CAS 单点登录）、党支部与用户管理、活动/新闻/公告、操作日志与 Excel 批量导入等 RESTful API。

- 身份体系：`super_admin`（超级管理员）/ `branch_admin`（支部管理员）/ `student`（普通成员）
- 接口契约：以 [`openapi.yaml`](openapi.yaml) 为准，在线文档 `http://localhost:8080/doc.html`

---

## 技术栈

| 层次 | 技术 | 版本 |
| ---- | ---- | ---- |
| 框架 | Spring Boot | 3.2.5 |
| 安全 | Spring Security + JWT (jjwt) | 0.12.6 |
| ORM | MyBatis-Plus | 3.5.5 |
| 数据库 | MySQL | 8.0+ |
| 缓存/验证码 | Redis（验证码、refresh token 白名单） | — |
| 日志检索 | Elasticsearch（操作日志；未配置时降级为应用日志） | — |
| 单点登录 | 学校 CAS（pac4j + HttpClient5 + dom4j） | 6.5.4 |
| 邮件 | Spring Mail（邮箱验证码） | — |
| Excel | Apache POI | 5.2.5 |
| 接口文档 | Knife4j (OpenAPI 3) | 4.4.0 |
| 工具 | Lombok、Jakarta Validation、MapStruct | — |
| 几何类型 | JTS Topology Suite（活动地点 `point`） | 1.19.0 |
| JDK | Java 21 | — |

---

## 项目结构

```
├── sql/                                  # 数据库脚本与文档
│   ├── databases.sql                     # 一键建库（按外键顺序 SOURCE 21 张表）
│   ├── tb_*.sql                          # 单表 DDL（20 个）
│   ├── alter_*.sql                       # 已有环境的幂等增量脚本
│   ├── schema-verify.sh                  # 在临时库重跑全套 DDL 的回归护栏
│   └── er-diagram.md                     # 20 张表的 Mermaid ER 图
├── docs/
│   ├── agents/                           # 技能套件的每仓配置（issue tracker / 标签 / 域文档布局）
│   └── research/                         # 调研笔记（如 API 版本与契约调研）
├── src/main/java/sicau/policialPartyManager/
│   ├── PolicialPartyManagerApplication.java
│   ├── api/
│   │   ├── controller/                   # 13 个控制器（见下方 API 概览）
│   │   ├── dto/                          # 请求/响应对象（Result、PageResult、各类 *Request/*Vo）
│   │   └── handler/                      # GlobalExceptionHandler
│   ├── config/                           # Security、MyBatis-Plus、Knife4j、CAS、异步、@CurrentUser
│   ├── filter/                           # JwtAuthFilter（解析 token 并装配权限）
│   ├── log/                              # 操作日志：Filter + 事件发布 + ES 监听器
│   ├── model/
│   │   ├── entity/                       # 20 个实体，与 sql/ 一一对应
│   │   ├── enums/UserRole.java           # 系统角色枚举
│   │   └── records/User.java             # TokenUser（JWT 中的用户信息）
│   ├── repository/                       # MyBatis-Plus Mapper（20 个）
│   ├── service/                          # 服务接口
│   │   ├── Impl/                         # 实现（Auth/Sso/Branch/Content/User/Admin*）
│   │   └── runner/                       # 启动初始化（超级管理员）
│   └── utils/                            # JwtUtil、CodeUtil、MailUtil、ExcelUtil
└── pom.xml
```

> 包结构已按 `api / config / filter / log / model / repository / service / utils` 分层，不再有早期的 `common/`、`security/` 包。

---

## 快速开始

### 1. 环境要求

- **JDK 21**
- **MySQL 8.0+**（必需）
- **Redis**（验证码与 refresh token；缺失时相关功能不可用）
- **Elasticsearch**（可选：操作日志存储；未配置时自动降级到应用日志）
- **Maven 3.6+**（或使用仓库内 `mvnw`）

### 2. 创建数据库

```bash
mysql -u root -p < sql/databases.sql
```

> 脚本会建库并按外键依赖顺序创建 21 张表。改过任何 `sql/tb_*.sql` 后，执行 `./sql/schema-verify.sh` 可验证整套 DDL 仍能干净建库（它会 DROP 并重建临时库 `pp_schema_verify`，不会碰业务库）。

### 3. 修改配置

真实凭据放在 **`src/main/resources/application-secret.yaml`**（已在 `.gitignore` 中），通过 `spring.profiles.include: secret` 导入，例如：

```yaml
DB_USERNAME: root
DB_PASSWORD: 你的密码
MAIL_HOST: smtp.qq.com
MAIL_USERNAME: your-account@qq.com
MAIL_PASSWORD: 授权码
REDIS_HOST: localhost
REDIS_PORT: 6379
ES_USERNAME: ""
ES_PASSWORD: ""
```

数据库名默认 `policial_party_manager`，可用环境变量 `DB_USERNAME` / `DB_PASSWORD` 覆盖。

### 4. 启动

```bash
mvn spring-boot:run
# 或
./mvnw spring-boot:run
```

首次启动会创建超级管理员账号（见「测试账号」）。

### 5. 访问

| 资源 | 地址 |
| ---- | ---- |
| API 服务 | `http://localhost:8080` |
| Knife4j 文档 | `http://localhost:8080/doc.html` |

---

## 认证与授权

### 认证方式（`/api/v1/auth`，唯一免登录前缀）

| 方式 | 路径 | 说明 |
| ---- | ---- | ---- |
| 用户名密码 | `POST /api/v1/auth/login/userpass` | 传统登录 |
| 邮箱验证码 | `POST /api/v1/auth/login/email` | 先调 `/verifyCode` 发码 |
| 手机号验证码 | `POST /api/v1/auth/login/phone` | **短信通道未接入**，验证码只写日志 |
| 学校 CAS SSO | `GET /api/v1/auth/sso/login` → `GET /api/v1/auth/sso/callback` | 未配置 `cas.server-url` 时提示「CAS 未配置」 |
| 刷新令牌 | `POST /api/v1/auth/refresh` | 用 refresh token 换 access token |
| 退出 | `POST /api/v1/auth/logout` | 吊销 refresh token |

### RBAC 模型

```
User ──N:M── Role ──N:M── Permission
```

- 用户与角色：`tb_user_role`
- 角色与权限：`tb_role_permission`（权限以字符串名存储）
- JWT 中携带 `userId`、`username`、`role`；`JwtAuthFilter` 装配为 `ROLE_<大写角色码>` 权限
- 方法级鉴权使用 `@PreAuthorize("hasRole('SUPER_ADMIN')")` 这类写法，**不要**自己再写 `ROLE_` 前缀（`hasRole` 会自动补）

### 系统角色

| 角色 | 数据库编码 | 权限范围 |
| ---- | ---- | ---- |
| 超级管理员 | `super_admin` | 全部后台接口（`/api/v4/admin/**`） |
| 支部管理员 | `branch_admin` | 目前仅 `/api/v2/branch/**`；细分数据隔离尚未全面落地 |
| 普通成员 | `student` | 个人中心与内容查询 |

### 测试账号

首次启动由 `InitSuperAdminRunner` 创建：

| 用户名 | 密码 | 角色 |
| ---- | ---- | ---- |
| `superadmin` | `123456` | 超级管理员 |

其余账号请用超级管理员登录后，通过 `/api/v4/admin/users`（新增）或 `/api/v4/admin/users/import`（Excel 批量导入）创建。业务库中已存在的账号不会被动。

### 调用方式

```
Authorization: Bearer <token>
```

在 Knife4j 文档页点击 **Authorize** 可全局设置。

---

## API 概览

**完整契约以 [`openapi.yaml`](openapi.yaml) 为准**（65 条 path / 83 个 operation），`doc.html` 是它的可视化版本。README 只给出模块地图，不逐条重抄——重抄必然再次落后于代码。

| 前缀 | 控制器 | 规模 | 鉴权 | 职责 |
| ---- | ---- | ---- | ---- | ---- |
| `/api/v1/auth` | `AuthController` | 8 path | **免登录** | 登录、验证码、CAS SSO、刷新、退出 |
| `/api/v2/content` | `ContentController` | 7 path | 登录即可 | 新闻/公告/活动的只读查询，只返回已发布且在展示期内的内容 |
| `/api/v2/branch` | ~~`BranchController`~~ | 3 path / 5 op | `branch_admin` | **已标废弃**（#51），能力全部由 `/api/v4/admin/branches/*` 覆盖，观察一个迭代后删除 |
| `/api/v4/admin` | `AdminController` + `AdminSignRecordController` + `AdminActivitySignupController` + `AdminThoughtReportController` | 37 path / 51 op | `super_admin` | 用户、支部、角色权限、活动、新闻、公告、签到/报名/思想汇报记录、操作日志、Excel 导入 |
| `/api/v4/user` | `UserController` | 1 path / 2 op | 登录即可 | 个人中心：查看/编辑本人资料 |
| `/api/v4/activities` | `ActivitySignController` + `ActivitySignupController` | 2 path / 3 op | 登录即可 | 成员签到与报名/取消（v4 前缀下按资源独立成类） |
| `/api/v4/thought-reports` | `ThoughtReportController` | 3 path / 3 op | 登录即可 | 思想汇报：提交（自动派审秘书）/我的列表/撤回 |
| `/api/v4/review/thought-reports` | `ThoughtReportReviewController` | 3 path / 3 op | `branch_admin` | 审阅侧：待审列表/详情/通过驳回（越权=「不存在」） |
| `/api/v4/uploads` | `UploadController` | 1 path / 1 op | 登录即可 | 本地附件上传（白名单扩展名，返回 file: 路径） |

补充说明：

- **历史记录**就是操作日志，统一走 `/api/v4/admin/logs/{page,stats,cleanup}`，没有独立的 `/api/v4/history` 入口。
- **导入模板**：`GET /api/v4/admin/{users,branches}/template` 下载的列与 Swagger 描述由契约测试保证一致。
- `/api/v2/branch` 已标废弃（控制器与服务均有 `@Deprecated`，openapi 5 个 operation 标 `deprecated: true`）。**新代码一律使用 `/api/v4/admin/branches/*`**。`DeprecatedEndpointContractTest` 锁住废弃标记的完整性：v2/branch 下漏标、或 v4 被误标，测试都会红。前端盘点见 [docs/research/2026-10-api-versioning-contract.md](docs/research/2026-10-api-versioning-contract.md)。

---

## 统一响应格式

```json
// 成功
{ "code": 200, "message": "success", "data": { ... } }

// 失败
{ "code": 400, "message": "错误描述", "data": null }

// 分页（data 内）
{ "records": [ ... ], "total": 100, "page": 1, "size": 10 }
```

---

## 数据库设计

共 **21 张表**，按四个域组织。完整字段与约束见 [sql/er-diagram.md](sql/er-diagram.md)（Mermaid，可在 VS Code 预览或粘贴到 mermaid.live）。

| 域 | 表 |
| --- | --- |
| 用户权限 | `tb_user`、`tb_user_detail`、`tb_user_role`、`tb_role`、`tb_role_permission` |
| 组织与身份 | `tb_branch`、`tb_identity`、`tb_identity_change` |
| 党建业务 | `tb_activity`、`tb_activity_detail`、`tb_activity_identity`、`tb_activity_signup`、`tb_activity_type`、`tb_sign_record`、`tb_study`、`tb_study_record`、`tb_thought_report` |
| 内容与日志 | `tb_news`、`tb_notice`、`tb_notice_top`、`tb_operation` |

**政治身份级别**（`tb_identity.level`）：

| Level | 身份 | 说明 |
| ---- | ---- | ---- |
| 1 | 普通学生 | 非党员 |
| 2 | 入党申请人 | 已提交申请 |
| 3 | 积极分子 | 已确定为积极分子 |
| 4 | 发展对象 | 已确定为发展对象 |
| 5 | 预备党员 | 已批准预备 |
| 6 | 正式党员 | 已转正 |

> `tb_activity_identity` 承载活动的目标人群范围（身份/年级/学院）；报名记录本身在 `tb_activity_signup`（身份维度校验待 #44 用户身份落库后补齐）。

---

## 配置说明

完整项见 [`src/main/resources/application.yaml`](src/main/resources/application.yaml)，这里只列需要你关心的：

```yaml
jwt:
  secret: <务必替换>                    # 生产环境必须换掉默认值
  access-token-expiration: 7200000      # 2 小时
  refresh-token-expiration: 604800000   # 7 天

cas:
  server-url: http://sso.example.edu.cn/cas/   # 未配置则 CAS 登录提示「未配置」

frontend:
  base-url: http://localhost:5173       # SSO 登录成功/失败后的回跳地址
  sso-landing-path: /sso/login

spring:
  data:
    redis: { host: localhost, port: 6379 }
  elasticsearch:
    uris: http://localhost:9200         # 未配置时日志写入降级为应用日志，不阻塞启动
  mail:
    host: smtp.qq.com                   # 未配置时不创建 JavaMailSender，验证码降级打印到日志
  profiles:
    include: secret                     # 导入 application-secret.yaml（真实凭据，已 gitignore）

app:
  upload:
    dir: ./data/uploads              # 本地附件存储根目录（#33 一期；OSS 后续平替），
                                      # 按 thought/yyyy/MM/uuid.ext 组织，库内存 file: 相对路径

mybatis-plus:
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted       # 预留配置：当前软删除走各表的 status 字段
```

---

## 开发约定

### 实体字段映射

- 数据库：`snake_case`；Java：`camelCase`，MyBatis-Plus 自动转换
- 实体与表一一对应，共 20 个；改表必须同步 `sql/tb_*.sql` 与实体

### 软删除

业务表用 `status` 标记：**`1` = 正常，`0` = 已删除**，查询统一加 `status = 1`。

> 注意各表 `status` 的取值域并不相同：`tb_user` / `tb_news` 用 `1/2`（启用/停用、上线/下线），`tb_notice` 用 `1/2/3`（正常/删除/延期展示），`tb_activity_detail` 用 `0/1/2`。新增表请沿用「**默认值 1，1 表示正常**」这一约定，并在 DDL 注释里写清取值含义。

### 自动填充

`createTime` 在 insert 时填充；`updateTime` 在 insert / update 时填充（仅当实体有该 setter）。

### @CurrentUser

Controller 中用 `@CurrentUser User user` 直接拿到当前登录用户（`userId` / `username` / `role`），无需自己从 `Authentication` 里取。

### API 版本约定

| 前缀 | 含义 | 状态 |
| ---- | ---- | ---- |
| `/api/v1/` | 认证入口（唯一免登录前缀） | 保留 |
| `/api/v2/content/` | 内容只读查询 | 保留 |
| `/api/v2/branch/` | 早期支部接口 | **已列入下线计划** |
| `/api/v4/` | 个人中心与管理员后台 | 当前主 surface |

**约定**（来自 [API 契约调研](docs/research/2026-10-api-versioning-contract.md)）：

1. 版本号**冻结**：不新增 `/api/v5`，新端点并入其所属域的现有前缀
2. `openapi.yaml` 是唯一对外契约；改接口必须同步它
3. 少数派前缀（`v1`/`v2`/`v4`）只是历史沿革，不代表 breaking change 的代数

### 改动后的自检

| 改了什么 | 跑什么 |
| --- | --- |
| `sql/*.sql` | `./sql/schema-verify.sh`（建库 + status 语义自检） |
| 控制器鉴权注解 | `mvn test`（契约测试会校验角色名存在、无空壳控制器） |
| 导入模板列 | `mvn test`（契约测试会校验 Swagger 描述与生成的 xlsx 表头一致） |
| 接口的废弃标记 | `mvn test`（契约测试会校验 `/api/v2/branch` 全部接口双标记齐全，且 v4 不被误标） |

---

## 待实现功能

**已实现**：认证（含 CAS）、用户/支部/角色/活动/新闻/公告后台、操作日志（ES）、Excel 批量导入、个人中心查询与编辑、活动签到与报名。

**尚未实现**：

- [x] `tb_sign_record` — 活动签到（成员签到 + 管理员按活动查记录；location 以 WKT 落 POINT）
- [x] `tb_activity_signup` — 活动报名（报名即生效、开始前可取消、名额与年级/学院校验；身份维度待 #44）
- [ ] 二维码 / token 签到、地理围栏
- [x] `tb_thought_report` — 思想汇报提交与审阅流（自动派审秘书 / 本地附件上传 / 状态机 1-2-3，见 #33）
- [ ] `tb_study` / `tb_study_record` — 学习课程与学习记录
- [ ] `tb_identity_change` — 政治身份变更的申请/审批链路（表已建，无业务层）
- [ ] 短信验证码通道（结构已留，仅写日志）
- [ ] 用户自助改密 / 忘记密码找回（目前只有管理员重置）
- [ ] 支部管理员的数据隔离（`branch_admin` 目前只能访问 v2 支部接口，未见按支部过滤的成员数据）

---

## 文档索引

| 文档 | 用途 |
| --- | --- |
| [`openapi.yaml`](openapi.yaml) | 接口契约（唯一权威） |
| [`sql/er-diagram.md`](sql/er-diagram.md) | 数据库 ER 与关键约束 |
| [`docs/agents/`](docs/agents/) | 技能套件配置：issue tracker、triage 标签、域文档布局 |
| [`docs/research/`](docs/research/) | 调研笔记（API 版本与契约） |
| [`AGENTS.md`](AGENTS.md) | 给 AI 协作代理的入口说明 |

---

## License

仅限学习与内部使用。
