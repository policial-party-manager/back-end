# 党建云平台 · Policial Party Manager

高校党建信息管理后端服务，基于 **Spring Boot 3.2 + MyBatis-Plus + Spring Security + JWT** 构建，提供党支部管理、成员管理、活动签到等功能的 RESTful API。

---

## 技术栈

| 层次 | 技术 | 版本 |
| ---- | ---- | ---- |
| 框架 | Spring Boot | 3.2.5 |
| 安全 | Spring Security + JWT (jjwt) | 0.12.6 |
| ORM | MyBatis-Plus | 3.5.5 |
| 数据库 | MySQL | 8.0+ |
| 日志存储 | Elasticsearch | — |
| 接口文档 | Knife4j (Swagger) | 4.4.0 |
| 工具 | Lombok, Jakarta Validation | — |
| JDK | Java 21 | — |

---

## 项目结构

```
dj/
├── sql/
│   ├── databases.sql          # 建库建表 DDL
│   └── er-diagram.md          # 数据库 ER 图（Mermaid）
├── src/main/java/sicau/policialPartyManager/
│   ├── PolicialPartyManagerApplication.java    # 启动入口
│   ├── common/
│   │   ├── Result.java              # 统一响应体 { code, message, data }
│   │   ├── PageResult.java          # 分页响应体 { total, page, size, records }
│   │   └── GlobalExceptionHandler.java  # 全局异常拦截
│   ├── config/
│   │   ├── SecurityConfig.java      # Spring Security 配置 + CORS
│   │   ├── JwtAuthFilter.java       # JWT 认证过滤器 + TokenUser 记录
│   │   ├── MybatisPlusConfig.java   # 分页插件 + 自动填充
│   │   ├── Knife4jConfig.java       # OpenAPI / Swagger 配置
│   │   ├── CurrentUser.java         # @CurrentUser 注解定义
│   │   ├── CurrentUserArgumentResolver.java  # 用户参数自动注入
│   │   ├── WebMvcConfig.java        # MVC 配置
│   │   └── DataInitializer.java     # 测试账号初始化
│   ├── api/
│   │   ├── controller/
│   │   │   ├── AuthController.java      # /api/v1/auth/*    认证（登录/验证码/SSO）
│   │   │   ├── UserController.java      # /api/v4/user/*    个人中心
│   │   │   ├── BranchController.java    # /api/v2/branch/*  党支部管理
│   │   │   ├── AdminController.java     # /api/v4/admin/*   管理员后台（用户/支部/角色/活动/新闻/公告/日志）
│   │   │   ├── ContentController.java   # /api/v2/content/* 内容公开查询（新闻/公告/活动）
│   │   │   └── HistoryController.java  # /api/v4/history/* 历史记录（操作日志）
│   │   └── dto/
│   │       ├── LoginRequest.java        # 登录请求（多模式复用）
│   │       ├── LoginResponse.java       # 登录响应（含 token、菜单）
│   │       ├── PageResult.java          # 分页响应
│   │       ├── Result.java              # 统一响应
│   │       └── ValidationGroups.java    # 校验分组
│   ├── log/                             # 统一操作日志（ES 存储 + 事件监听）
│   │   ├── OperationLog.java
│   │   ├── OperationLogPublisher.java
│   │   ├── OperationLogFilter.java
│   │   ├── OperationLogListener.java
│   │   ├── AuthContext.java
│   │   ├── LogType.java
│   │   └── RequestContext.java
│   ├── model/
│   │   ├── entity/                     # 实体类（对应数据库表）
│   │   │   ├── User.java               # tb_user
│   │   │   ├── UserDetail.java         # tb_user_detail（用户扩展字段）
│   │   │   ├── UserRole.java           # tb_user_role
│   │   │   ├── UserPermission.java     # tb_user_permission
│   │   │   ├── Role.java               # tb_role
│   │   │   ├── RolePermission.java     # tb_role_permission
│   │   │   ├── Permission.java         # tb_permission
│   │   │   ├── Branch.java             # tb_branch
│   │   │   ├── Identity.java           # tb_identity（政治身份）
│   │   │   ├── IdentityChange.java     # tb_identity_change
│   │   │   ├── Activity.java          # tb_activity
│   │   │   ├── ActivityDetail.java     # tb_activity_detail
│   │   │   ├── ActivityType.java      # tb_activity_type
│   │   │   ├── ActivityIdentity.java  # tb_activity_identity
│   │   │   ├── News.java              # tb_news
│   │   │   ├── Notice.java            # tb_notice
│   │   │   ├── NoticeTop.java         # tb_notice_top
│   │   │   ├── SignRecord.java        # tb_sign_record
│   │   │   ├── Study.java             # tb_study
│   │   │   ├── StudyRecord.java       # tb_study_record
│   │   │   ├── ThoughtReport.java     # tb_thought_report
│   │   │   └── Operation.java         # tb_operation
│   │   └── records/
│   │       └── User.java               # TokenUser（JWT 中存储的用户信息）
│   ├── repository/                     # MyBatis-Plus Mapper 接口
│   ├── security/
│   │   └── JwtUtil.java               # JWT 生成/解析工具
│   └── service/
│       ├── AuthService.java            # 认证服务
│       ├── SsoService.java             # 学校 CAS SSO 服务
│       ├── BranchService.java         # 党支部服务
│       ├── ContentService.java        # 内容只读服务（新闻/公告/活动）
│       ├── UserService.java          # 个人中心服务
│       └── Impl/
│           ├── AuthServiceImpl.java
│           ├── SsoServiceImpl.java
│           ├── BranchServiceImpl.java
│           ├── ContentServiceImpl.java
│           ├── UserServiceImpl.java
│           ├── AdminUserServiceImpl.java
│           ├── AdminBranchServiceImpl.java
│           ├── AdminRoleServiceImpl.java
│           ├── AdminActivityServiceImpl.java
│           ├── AdminNewsServiceImpl.java
│           ├── AdminNoticeServiceImpl.java
│           └── AdminOperationLogServiceImpl.java
└── pom.xml
```

---

## 快速开始

### 1. 环境要求

- **JDK 21**
- **MySQL 8.0+**
- **Elasticsearch 8.x**（操作日志存储，如不使用可注释掉相关 Bean）
- **Maven 3.6+**

### 2. 创建数据库

```bash
mysql -u root -p < sql/databases.sql
```

### 3. 修改配置

编辑 `src/main/resources/application.yaml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/party_manager?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: 你的密码
```

### 4. 启动

```bash
mvn spring-boot:run
```

首次启动自动创建测试账号和基础数据（角色、政治身份）。

### 5. 访问

| 资源 | 地址 |
| ---- | ---- |
| API 服务 | `http://localhost:8080` |
| 接口文档 | `http://localhost:8080/doc.html` |

---

## 认证与授权

### 认证方式

| 方式 | 路径 | 说明 |
| ---- | ---- | ---- |
| 用户名密码 | `POST /api/v1/auth/login/userpass` | 传统登录 |
| 邮箱验证码 | `POST /api/v1/auth/login/email` | 发送验证码后登录 |
| 手机号验证码 | `POST /api/v1/auth/login/phone` | 短信通道暂未接入 |
| 学校 CAS SSO | `GET /api/v1/auth/sso/login` | 跳转统一身份认证 |
| 刷新令牌 | `POST /api/v1/auth/refresh` | 换取新 access token |

### RBAC 模型

```
User ──N:M── Role ──N:M── Permission
```

- 用户与角色通过 `tb_user_role` 关联
- 角色与权限通过 `tb_role_permission` 关联
- JWT 中存储 `userId`、`username`、`role`

### 系统角色

| 角色 | 编码 | 权限范围 |
| ---- | ---- | ---- |
| 超级管理员 | `SUPER_ADMIN` | 全部党支部、全部成员、系统管理 |
| 支部管理员 | `BRANCH_ADMIN` | 仅本支部成员的管理 |
| 普通成员 | `STUDENT` | 仅查看自己的信息 |

### 测试账号

| 用户名 | 密码 | 角色 |
| ---- | ---- | ---- |
| `admin` | `123456` | SUPER_ADMIN |
| `branch1` | `123456` | BRANCH_ADMIN |
| `student1` | `123456` | STUDENT |

### 调用方式

```
Authorization: Bearer <token>
```

先调用登录接口获取 token，再将其填入后续请求的 Header 中。在 Knife4j 文档页点击 **Authorize** 按钮可全局设置。

---

## API 概览

### 认证 `[/api/v1/auth]`

| 方法 | 路径 | 说明 | 认证 |
| ---- | ---- | ---- | ---- |
| POST | `/api/v1/auth/login/userpass` | 用户名密码登录 | 否 |
| POST | `/api/v1/auth/login/email` | 邮箱验证码登录 | 否 |
| POST | `/api/v1/auth/login/phone` | 手机号验证码登录（短信未接入） | 否 |
| POST | `/api/v1/auth/verifyCode` | 发送验证码 | 否 |
| POST | `/api/v1/auth/refresh` | 刷新 JWT token | 否 |
| POST | `/api/v1/auth/logout` | 退出登录 | 是 |
| GET | `/api/v1/auth/sso/login` | CAS 登录跳转 | 否 |
| GET | `/api/v1/auth/sso/callback` | CAS 回调 | 否 |

### 个人中心 `[/api/v4/user]`

| 方法 | 路径 | 说明 | 认证 |
| ---- | ---- | ---- | ---- |
| GET | `/api/v4/user/profile` | 获取个人信息 | 是 |
| PUT | `/api/v4/user/profile` | 编辑个人信息 | 是 |

### 党支部管理 `[/api/v2/branch]` · 仅 BRANCH_ADMIN

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| GET | `/api/v2/branch/list` | 支部列表 |
| GET | `/api/v2/branch/{id}` | 支部详情 |
| POST | `/api/v2/branch` | 新增支部 |
| PUT | `/api/v2/branch/{id}` | 编辑支部 |
| DELETE | `/api/v2/branch/{id}` | 删除支部（软删除） |

### 内容公开查询 `[/api/v2/content]` · 登录即可访问

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| GET | `/api/v2/content/news/page` | 新闻列表（已上线，支持 keyword/类型过滤） |
| GET | `/api/v2/content/news/{id}` | 新闻详情（浏览量+1） |
| GET | `/api/v2/content/notices/page` | 公告列表（展示期内，支持 keyword 过滤） |
| GET | `/api/v2/content/notices/{id}` | 公告详情 |
| GET | `/api/v2/content/activities/page` | 活动列表（已发布，支持 keyword/支部/类型过滤） |
| GET | `/api/v2/content/activities/{id}` | 活动详情 |
| GET | `/api/v2/content/activity-types` | 活动类型列表 |

### 管理员后台 `[/api/v4/admin]` · 仅 SUPER_ADMIN

#### 用户管理

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| GET | `/api/v4/admin/users/page` | 用户分页列表 |
| GET | `/api/v4/admin/users/{id}` | 用户详情 |
| POST | `/api/v4/admin/users` | 新增用户 |
| PUT | `/api/v4/admin/users/{id}` | 编辑用户 |
| PUT | `/api/v4/admin/users/{id}/status` | 启用/停用用户 |
| PUT | `/api/v4/admin/users/{id}/password` | 重置用户密码 |
| GET | `/api/v4/admin/users/template` | 下载用户导入模板 |
| POST | `/api/v4/admin/users/import` | Excel 批量导入用户 |

#### 党支部管理

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| GET | `/api/v4/admin/branches/page` | 支部分页列表 |
| GET | `/api/v4/admin/branches/{id}` | 支部详情 |
| POST | `/api/v4/admin/branches` | 新增支部 |
| PUT | `/api/v4/admin/branches/{id}` | 编辑支部 |
| DELETE | `/api/v4/admin/branches/{id}` | 删除支部（软删除） |
| GET | `/api/v4/admin/branches/options` | 支部下拉列表 |
| GET | `/api/v4/admin/branches/template` | 下载支部导入模板 |
| POST | `/api/v4/admin/branches/import` | Excel 批量导入支部 |

#### 权限管理（角色）

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| GET | `/api/v4/admin/roles` | 角色列表 |
| GET | `/api/v4/admin/roles/{id}` | 角色详情 |
| POST | `/api/v4/admin/roles` | 新增角色 |
| PUT | `/api/v4/admin/roles/{id}` | 编辑角色 |
| DELETE | `/api/v4/admin/roles/{id}` | 删除角色 |
| GET | `/api/v4/admin/roles/permission-names` | 内置权限点字典 |
| PUT | `/api/v4/admin/roles/{id}/permissions` | 分配角色权限 |

#### 活动管理

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| GET | `/api/v4/admin/activities/page` | 活动分页列表 |
| GET | `/api/v4/admin/activities/{id}` | 活动详情 |
| POST | `/api/v4/admin/activities` | 新增活动 |
| PUT | `/api/v4/admin/activities/{id}` | 编辑活动 |
| DELETE | `/api/v4/admin/activities/{id}` | 删除活动 |
| PUT | `/api/v4/admin/activities/{id}/status` | 调整活动状态 |
| GET | `/api/v4/admin/activity-types` | 活动类型列表 |
| POST | `/api/v4/admin/activity-types` | 新增活动类型 |
| PUT | `/api/v4/admin/activity-types/{id}` | 编辑活动类型 |
| DELETE | `/api/v4/admin/activity-types/{id}` | 删除活动类型 |

#### 新闻管理

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| GET | `/api/v4/admin/news/page` | 新闻分页列表 |
| GET | `/api/v4/admin/news/{id}` | 新闻详情 |
| POST | `/api/v4/admin/news` | 新增新闻 |
| PUT | `/api/v4/admin/news/{id}` | 编辑新闻 |
| DELETE | `/api/v4/admin/news/{id}` | 删除新闻 |
| PUT | `/api/v4/admin/news/{id}/status` | 新闻发布/下线 |

#### 公告管理

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| GET | `/api/v4/admin/notices/page` | 公告分页列表 |
| GET | `/api/v4/admin/notices/{id}` | 公告详情 |
| POST | `/api/v4/admin/notices` | 新增公告 |
| PUT | `/api/v4/admin/notices/{id}` | 编辑公告 |
| DELETE | `/api/v4/admin/notices/{id}` | 删除公告 |
| PUT | `/api/v4/admin/notices/{id}/publish` | 发布公告 |

#### 操作日志（Elasticsearch）

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| GET | `/api/v4/admin/logs/page` | 操作日志分页检索 |
| GET | `/api/v4/admin/logs/stats` | 日志类型统计 |
| DELETE | `/api/v4/admin/logs/cleanup` | 清理过期日志 |

### 历史记录 `[/api/v4/history]` · 仅 SUPER_ADMIN

> 暂未实现。

---

## 统一响应格式

```json
// 成功
{ "code": 200, "message": "success", "data": { ... } }

// 失败
{ "code": 400, "message": "错误描述", "data": null }

// 分页
{
  "code": 200, "message": "success",
  "data": { "total": 100, "page": 1, "size": 10, "records": [...] }
}
```

---

## 数据库设计

共 20+ 张表，分为以下模块：

```
┌──────────────┐   ┌──────────────┐   ┌────────────────────┐
│  认证与权限   │   │   组织架构    │   │     党建业务        │
├──────────────┤   ├──────────────┤   ├────────────────────┤
│ tb_user       │   │ tb_branch     │   │ tb_activity         │
│ tb_user_detail│   │ tb_identity   │   │ tb_activity_detail  │
│ tb_role       │   │ tb_identity_change │ tb_activity_type   │
│ tb_permission │   └──────────────┘   │ tb_activity_identity│
│ tb_user_role  │                       │ tb_sign_record      │
│ tb_user_permission                    │ tb_news             │
│ tb_role_permission│                   │ tb_notice           │
└──────────────┘   ┌──────────────┐   │ tb_notice_top      │
┌──────────────┐   │   学习档案    │   └────────────────────┘
│  操作日志     │   ├──────────────┤
│ tb_operation  │   │ tb_study      │
└──────────────┘   │ tb_study_record│
                   │ tb_thought_report
                   └──────────────┘
```

ER 图详见 [sql/er-diagram.md](sql/er-diagram.md)，可在 VS Code 中安装 Mermaid 插件预览，或复制到 [mermaid.live](https://mermaid.live) 导出图片。

**政治身份级别**（`tb_identity.level`）：

| Level | 身份 | 说明 |
| ---- | ---- | ---- |
| 1 | 普通学生 | 非党员 |
| 2 | 入党申请人 | 已提交申请 |
| 3 | 积极分子 | 已确定为积极分子 |
| 4 | 发展对象 | 已确定为发展对象 |
| 5 | 预备党员 | 已批准预备 |
| 6 | 正式党员 | 已转正 |

---

## 配置说明

```yaml
# JWT
jwt:
  secret: your-256-bit-secret-key    # 生产环境务必修改
  expiration: 86400000               # token 有效期（毫秒），默认 24h

# CAS SSO
cas:
  base-url: https://your-school-cas.edu.cn
  service-ticket-validate-url: /cas/serviceValidate

# 前端回跳地址（SSO 登录成功/失败后重定向）
frontend:
  base-url: http://localhost:5173
  sso-landing-path: /sso/login

# Elasticsearch（日志存储）
spring:
  elasticsearch:
    uris: http://localhost:9200

# MyBatis-Plus
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true   # 下划线自动转驼峰
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl  # SQL 日志
  global-config:
    db-config:
      id-type: auto                      # 主键自增
      logic-delete-field: deleted        # 逻辑删除字段（预留）
```

---

## 开发约定

### 实体字段映射

- 数据库字段：`snake_case`（如 `real_name`、`branch_id`）
- Java 字段：`camelCase`（如 `realName`、`branchId`）
- MyBatis-Plus 自动转换，无需手动映射

### 软删除

业务表使用 `status` 字段标记删除：`1` = 正常，`0` = 已删除。查询时统一加 `status = 1` 条件。

### 自动填充

`createTime` 在 insert 时自动填充；`updateTime` 在 insert / update 时自动填充（仅适用于有该字段的实体：`tb_user`）。

### @CurrentUser

Controller 中通过 `@CurrentUser User user` 直接获取当前登录用户，无需手动从 `Authentication` 中提取。

### API 版本约定

| 前缀 | 说明 |
| ---- | ---- |
| `/api/v1/` | 认证相关 |
| `/api/v2/` | 公开内容查询、党支部管理 |
| `/api/v4/` | 个人中心、管理员后台、历史记录 |

---

## 待实现功能

- [x] `tb_activity` — 党建活动 CRUD（含活动类型）
- [x] `tb_news` — 新闻管理
- [x] `tb_notice` — 公告管理
- [x] 操作日志（ES 存储 + 事件监听解耦）
- [x] Excel 批量导入（用户、支部）
- [ ] `tb_sign_record` — 签到记录（含二维码签到）
- [ ] `tb_activity_member` — 活动报名 / 成员关联
- [ ] `tb_study` / `tb_study_record` — 学习记录
- [ ] `tb_thought_report` — 思想汇报
- [ ] `tb_identity_change` — 政治身份变迁记录
- [ ] 密码修改（用户自助）
- [ ] 密码重置（忘记密码）

---

## License

仅限学习与内部使用。
