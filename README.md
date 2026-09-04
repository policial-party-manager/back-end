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
│   │   ├── PageResult.java          # 分页响应体 { total, page, size, rows }
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
│   ├── controller/
│   │   ├── AuthController.java      # /api/auth/*      认证
│   │   ├── BranchController.java    # /api/branch/*    党支部管理
│   │   ├── MemberController.java    # /api/member/*    成员管理
│   │   └── UserController.java      # /api/user/*      个人中心
│   ├── dto/
│   │   ├── LoginRequest.java        # 登录请求
│   │   ├── LoginResponse.java       # 登录响应（含 token、菜单）
│   │   └── MenuVo.java              # 前端菜单项
│   ├── entity/
│   │   ├── User.java                # tb_user
│   │   ├── Role.java                # tb_role
│   │   ├── Permission.java          # tb_permission
│   │   ├── UserRole.java            # tb_user_role
│   │   ├── RolePermission.java      # tb_role_permission
│   │   ├── Branch.java              # tb_branch
│   │   ├── Identity.java            # tb_identity
│   │   ├── Member.java              # tb_member
│   │   └── MemberIdentity.java      # 政治身份枚举（参考）
│   ├── repository/                  # MyBatis-Plus Mapper 接口
│   │   ├── UserMapper.java
│   │   ├── RoleMapper.java
│   │   ├── PermissionMapper.java
│   │   ├── UserRoleMapper.java
│   │   ├── RolePermissionMapper.java
│   │   ├── BranchMapper.java
│   │   ├── IdentityMapper.java
│   │   └── MemberMapper.java
│   ├── security/
│   │   ├── JwtUtil.java             # JWT 生成/解析工具
│   │   └── UserRole.java            # 系统角色枚举（参考）
│   └── service/
│       ├── AuthService.java         # 认证 + RBAC 角色查询
│       ├── BranchService.java       # 党支部 CRUD
│       └── MemberService.java       # 成员 CRUD + 权限隔离
└── pom.xml
```

---

## 快速开始

### 1. 环境要求

- **JDK 21**
- **MySQL 8.0+**
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
| 超级管理员 | `super_admin` | 全部党支部、全部成员、系统管理 |
| 支部管理员 | `branch_admin` | 仅本支部成员的管理 |
| 普通成员 | `student` | 仅查看自己的信息 |

### 测试账号

| 用户名 | 密码 | 角色 |
| ---- | ---- | ---- |
| `admin` | `123456` | super_admin |
| `branch1` | `123456` | branch_admin |
| `student1` | `123456` | student |

### 调用方式

```
Authorization: Bearer <token>
```

先调用 `POST /api/auth/login` 获取 token，再将其填入后续请求的 Header 中。在 Knife4j 文档页点击 **Authorize** 按钮可全局设置。

---

## API 概览

### 认证 `[/api/auth]`

| 方法 | 路径 | 说明 | 认证 |
| ---- | ---- | ---- | ---- |
| POST | `/api/auth/login` | 登录，返回 token + 菜单 | 否 |
| GET | `/api/auth/current` | 当前用户信息 | 是 |

### 党支部管理 `[/api/branch]` · 仅 SUPER_ADMIN

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| GET | `/api/branch/list` | 支部列表 |
| GET | `/api/branch/{id}` | 支部详情 |
| POST | `/api/branch` | 新增支部 |
| PUT | `/api/branch/{id}` | 编辑支部 |
| DELETE | `/api/branch/{id}` | 删除支部（软删除） |

### 成员管理 `[/api/member]`

| 方法 | 路径 | 说明 | 权限 |
| ---- | ---- | ---- | ---- |
| GET | `/api/member/list` | 分页列表（支持 keyword/college/identityId 筛选） | 全员（数据隔离） |
| GET | `/api/member/{studentNo}` | 成员详情 | 全员（数据隔离） |
| POST | `/api/member` | 新增成员 | ADMIN |
| PUT | `/api/member/{studentNo}` | 编辑成员 | ADMIN |
| DELETE | `/api/member/{studentNo}` | 删除成员（软删除） | ADMIN |

> **数据隔离规则**：`branch_admin` 只看到本支部成员，`student` 只看到自己。

### 个人中心 `[/api/user]`

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| GET | `/api/user/profile` | 个人信息（账号 + 成员档案） |
| PUT | `/api/user/profile` | 编辑联系方式（phone/email） |

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
  "data": { "total": 100, "page": 1, "size": 10, "rows": [...] }
}
```

---

## 数据库设计

共 11 张表，分为四个模块：

```
┌──────────────┐   ┌──────────────┐   ┌────────────────────┐
│  认证与权限   │   │   组织架构    │   │     党建业务        │
├──────────────┤   ├──────────────┤   ├────────────────────┤
│ tb_user       │   │ tb_branch     │   │ tb_activity         │
│ tb_role       │   │ tb_member     │   │ tb_activity_member  │
│ tb_permission │   │ tb_identity   │   │ tb_sign_record      │
│ tb_user_role  │   └──────────────┘   └────────────────────┘
│ tb_role_perm  │
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

Controller 中通过 `@CurrentUser TokenUser user` 直接获取当前登录用户，无需手动从 `Authentication` 中提取。

---

## 待实现功能

- [ ] `tb_activity` — 党建活动 CRUD
- [ ] `tb_activity_member` — 活动报名 / 成员关联
- [ ] `tb_sign_record` — 签到记录（含二维码签到）
- [ ] `tb_permission` + `tb_role_permission` — 细粒度权限控制
- [ ] 操作日志（AOP）
- [ ] 数据导入导出（Excel）
- [ ] 密码修改、重置功能

---

## License

仅限学习与内部使用。
