# 数据库表结构分析与修改建议

> 对比基准：`databases.sql`（重构目标） vs `tb*.sql`（21 个独立文件，当前状态）

---

## 一、总体架构对比

| 维度 | `databases.sql` | `tb*.sql`（21 个独立文件） |
|---|---|---|
| 表数量 | **22 张表** | **21 张表** |
| 架构设计 | 六大模块清晰分层，注释完整 | 零散独立文件，无整体架构说明 |
| 编码规范 | 统一大写关键字 + 统一注释风格 | 小写关键字，风格不统一 |
| 初始数据 | 含种子数据 (INSERT) | 无 |
| 外键策略 | 规范完整 | 部分缺失 / 冗余 |

---

## 二、databases.sql 有而 tb*.sql 缺失的表（7 张）

| # | 缺失表 | 用途 | 影响 |
|---|---|---|---|
| 1 | **`tb_permission`** | 权限字典表 | tb*.sql 把权限当字符串散落在关联表中，无法做权限的集中管理和动态分配 |
| 2 | **`tb_member`** | 成员信息核心表 | tb*.sql 把成员信息拆在 `tb_user` + `tb_user_detail` 里，缺少统一的"党员/成员"抽象 |
| 3 | **`tb_cultivation_contact`** | 培养联系人 | 整条培养联系人链路缺失 |
| 4 | **`tb_cultivation_material`** | 培养材料表 | 没有档案材料管理 |
| 5 | **`tb_party_school_record`** | 党校学习记录 | tb*.sql 用 `tb_study` + `tb_study_record` 替代，但缺少学时/结业证书/状态等关键字段 |
| 6 | **`tb_activity_member`** | 活动参与表 | 无法记录"谁报名了哪个活动、参与状态" |
| 7 | **`tb_resource`** | 资源下载 | 无模板/资料下载功能 |
| 8 | **`tb_dict`** | 数据字典 | 无统一字典管理，枚举值硬编码 |

---

## 三、逐表修改建议

### 🔴 高优先级（阻塞性问题）

---

#### 3.1 `tb_user.sql` — 字段长度 + 密码安全

**当前问题：**

| 问题 | 详情 |
|---|---|
| `username varchar(10)` | 10 字符存不下学号/工号 |
| `password varchar(64)` | BCrypt 密文需 60 字符，加盐后可能超 64 |
| `password default (md5('123456'))` | MD5 不安全，不应有默认密码 |
| 缺少关键字段 | 无 `user_no`、`real_name`、`phone`、`email` |
| `last_login_time default CURRENT_TIMESTAMP` | 首次登录时间应为 NULL |

**建议修改：**

```sql
CREATE TABLE tb_user (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    user_no         VARCHAR(50)  NOT NULL UNIQUE       COMMENT '统一身份号（学号/工号）',
    username        VARCHAR(50)  NOT NULL UNIQUE       COMMENT '登录账号',
    password        VARCHAR(255)                       COMMENT '密码（BCrypt加密）',
    real_name       VARCHAR(50)  NOT NULL              COMMENT '姓名',
    phone           VARCHAR(20)                        COMMENT '手机号',
    email           VARCHAR(100)                       COMMENT '邮箱',
    status          TINYINT      DEFAULT 1             COMMENT '状态 1启用/2停用',
    last_login_time DATETIME                           COMMENT '最近一次登录',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT chk_user_status CHECK (`status` IN (1, 2))
) COMMENT '用户表';

CREATE INDEX idx_user_status   ON tb_user (status);
CREATE INDEX idx_user_username ON tb_user (username);
```

---

#### 3.2 `tb_role.sql` — 约束过严 + 缺少编码

**当前问题：**

| 问题 | 详情 |
|---|---|
| `CHECK (role_name LIKE 'ROLE_%')` | 强制命名前缀不合理 |
| 缺少 `role_code` | 没有唯一编码字段，只能靠名称做权限判断 |
| `role_name varchar(10)` | 太小 |

**建议修改：**

```sql
CREATE TABLE tb_role (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '角色ID',
    role_name   VARCHAR(50)  NOT NULL              COMMENT '角色名称',
    role_code   VARCHAR(50)  NOT NULL UNIQUE       COMMENT '角色编码 super_admin/branch_admin/student',
    description VARCHAR(255)                       COMMENT '角色描述'
) COMMENT '角色表';
```

---

#### 3.3 `tb_identity.sql` — 长度不足 + 不应耦合角色

**当前问题：**

| 问题 | 详情 |
|---|---|
| `identity_name varchar(10)` | "入党申请人" = 15 字节，已超限 |
| `role_id` 外键 | 把"政治身份"和"系统角色"两个概念耦合 |
| 缺少 `identity_code` | 无唯一编码 |

**建议修改：**

```sql
CREATE TABLE tb_identity (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '身份ID',
    identity_name VARCHAR(50)  NOT NULL              COMMENT '身份名称',
    identity_code VARCHAR(50)  NOT NULL UNIQUE       COMMENT '身份编码',
    level         INT                                COMMENT '发展阶段 1普通学生/2入党申请人/3积极分子/4发展对象/5预备党员/6正式党员',
    description   VARCHAR(255)                       COMMENT '描述'
) COMMENT '政治身份字典表';
```

---

#### 3.4 `tb_news.sql` — 标题 + 类型长度严重不足

**当前问题：**

| 问题 | 详情 |
|---|---|
| `title varchar(10)` | 新闻标题 10 字符绝不可能够 |
| `type varchar(10)` | 用关键字作列名 + 太小 |
| `view_count DEFAULT 1` | 默认值应为 0 |
| `status tinyint` | 用数字枚举，可读性差 |
| `create_time / update_time` | 无默认值 |

**建议修改：**

```sql
CREATE TABLE tb_news (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '新闻ID',
    title        VARCHAR(200) NOT NULL              COMMENT '标题',
    cover_url    VARCHAR(500)                       COMMENT '封面图URL',
    summary      VARCHAR(500)                       COMMENT '摘要',
    content      LONGTEXT                           COMMENT '正文（富文本）',
    category     VARCHAR(30)  DEFAULT 'NEWS'        COMMENT '分类 NEWS/ACHIEVEMENT',
    author       VARCHAR(50)                        COMMENT '作者署名',
    publisher_id BIGINT                             COMMENT '发布人(用户ID)',
    view_count   INT          DEFAULT 0             COMMENT '浏览次数',
    status       VARCHAR(20)  DEFAULT 'DRAFT'       COMMENT '状态 DRAFT/PUBLISHED',
    publish_time DATETIME                           COMMENT '发布时间',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (publisher_id) REFERENCES tb_user(id),
    CONSTRAINT chk_news_status CHECK (`status` IN ('DRAFT', 'PUBLISHED'))
) COMMENT '新闻表';
```

---

#### 3.5 `tb_thought_report.sql` — 缺少重要字段 + 类型不足

**当前问题：**

| 问题 | 详情 |
|---|---|
| `title varchar(10)` | 标题太短 |
| `status tinyint` | 应用 VARCHAR 语义值 |
| `file varchar(50)` | 应改为 `file_url` 支持 MinIO 长 URL |
| `review_remark varchar(50)` | 审阅意见可能超 50 字 |
| 缺少 `member_id` | 应关联成员而非 user（解耦用户账号和党员身份） |

**建议修改：**

```sql
CREATE TABLE tb_thought_report (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '汇报ID',
    member_id     BIGINT       NOT NULL              COMMENT '成员ID',
    title         VARCHAR(200) NOT NULL              COMMENT '汇报标题',
    content       TEXT                               COMMENT '汇报内容',
    file_url      VARCHAR(500)                       COMMENT '附件URL（MinIO）',
    reviewer_id   BIGINT                             COMMENT '审阅人(用户ID)',
    review_status VARCHAR(20)  DEFAULT 'PENDING'     COMMENT '审阅状态 PENDING/APPROVED/REJECTED',
    review_remark VARCHAR(500)                       COMMENT '审阅意见',
    submit_time   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    review_time   DATETIME                           COMMENT '审阅时间',
    FOREIGN KEY (member_id)   REFERENCES tb_member(id),
    FOREIGN KEY (reviewer_id) REFERENCES tb_user(id)
) COMMENT '思想汇报表';
```

---

#### 3.6 `tb_operation.sql` — 审计字段严重不足

**当前问题：**

| 问题 | 详情 |
|---|---|
| 缺少 `module / action / target_type / target_id` | 无法追踪具体操作 |
| 缺少 `before_data / after_data (JSON)` | 无法做变更对比 |
| 缺少 `error_msg` | 无法记录失败原因 |
| `ip_address varchar(10)` | IPv6 可长达 45 字符 |
| `user_agent varchar(10)` | 完全不够 |

**建议修改：**

```sql
CREATE TABLE tb_operation_log (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    operator_id   BIGINT       NOT NULL              COMMENT '操作人(用户ID)',
    module        VARCHAR(50)                        COMMENT '操作模块',
    action        VARCHAR(50)  NOT NULL              COMMENT '操作类型 CREATE/UPDATE/DELETE/IMPORT/EXPORT',
    target_type   VARCHAR(50)                        COMMENT '操作对象类型',
    target_id     VARCHAR(50)                        COMMENT '操作对象ID',
    before_data   JSON                               COMMENT '修改前数据',
    after_data    JSON                               COMMENT '修改后数据',
    ip_address    VARCHAR(50)                        COMMENT '操作IP',
    user_agent    VARCHAR(500)                       COMMENT '浏览器UA',
    result        VARCHAR(20)  DEFAULT 'SUCCESS'     COMMENT '操作结果 SUCCESS/FAIL',
    error_msg     VARCHAR(1000)                      COMMENT '错误信息',
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    FOREIGN KEY (operator_id) REFERENCES tb_user(id)
) COMMENT '操作日志表';
```

---

### 🟡 中优先级（设计改进）

---

#### 3.7 `tb_branch.sql` — 长度 + 缺少字段

**当前问题：**

| 问题 | 详情 |
|---|---|
| `college varchar(10)` | "计算机与信息工程学院" 远不止 10 字节 |
| `branch_name varchar(20)` | 偏小 |
| 缺少 `status` | 无法停用支部 |

**建议修改：**

```sql
CREATE TABLE tb_branch (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '支部ID',
    branch_name  VARCHAR(100) NOT NULL              COMMENT '支部名称',
    college      VARCHAR(100)                       COMMENT '所属学院',
    secretary_id BIGINT                             COMMENT '支部书记(用户ID)',
    description  TEXT                               COMMENT '支部简介',
    status       TINYINT      DEFAULT 1             COMMENT '状态 1启用/0停用',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (secretary_id) REFERENCES tb_user(id)
) COMMENT '党支部表';
```

---

#### 3.8 `tb_activity.sql` + `tb_activity_detail.sql` + `tb_activity_identity.sql` → 合并

**当前问题：** 活动拆成 3 张表，每次列表查询都要 JOIN；且各表字段长度不足。

**建议：** 合并为一张主表：

```sql
CREATE TABLE tb_activity (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '活动ID',
    title           VARCHAR(200) NOT NULL              COMMENT '活动名称',
    type_id         BIGINT                             COMMENT '活动类型ID',
    description     TEXT                               COMMENT '活动简介',
    cover_url       VARCHAR(500)                       COMMENT '活动封面URL',
    branch_id       BIGINT                             COMMENT '主办支部',
    creator_id      BIGINT                             COMMENT '创建人(用户ID)',
    location        VARCHAR(200)                       COMMENT '活动地点',
    location_detail VARCHAR(200)                       COMMENT '地点文字说明',
    start_time      DATETIME                           COMMENT '活动开始时间',
    end_time        DATETIME                           COMMENT '活动结束时间',
    sign_start      DATETIME                           COMMENT '签到开始时间',
    sign_end        DATETIME                           COMMENT '签到截止时间',
    target_identity VARCHAR(100)                       COMMENT '目标身份（逗号分隔identity_id）',
    target_grade    INT                                COMMENT '目标年级',
    target_college  VARCHAR(100)                       COMMENT '目标学院',
    max_participants INT                               COMMENT '人数上限（NULL=不限）',
    status          VARCHAR(30)  DEFAULT 'DRAFT'       COMMENT '状态 DRAFT/PUBLISHED/IN_PROGRESS/FINISHED/ARCHIVED',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (type_id)    REFERENCES tb_activity_type(id),
    FOREIGN KEY (branch_id)  REFERENCES tb_branch(id),
    FOREIGN KEY (creator_id) REFERENCES tb_user(id)
) COMMENT '活动表';
```

> ✅ 可删除：`tb_activity_detail.sql`、`tb_activity_identity.sql`

---

#### 3.9 `tb_activity_type.sql` — 缺少编码

**当前问题：** 缺少 `code` 唯一编码字段。

**建议修改：**

```sql
CREATE TABLE tb_activity_type (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '类型ID',
    name          VARCHAR(50)   NOT NULL              COMMENT '类型名称',
    code          VARCHAR(50)   NOT NULL UNIQUE       COMMENT '类型编码',
    need_sign     TINYINT       DEFAULT 1             COMMENT '是否需要签到',
    count_to_file TINYINT       DEFAULT 1             COMMENT '是否计入培养档案'
) COMMENT '活动类型字典表';
```

---

#### 3.10 `tb_sign_record.sql` — 坐标 + 字段长度

**当前问题：**

| 问题 | 详情 |
|---|---|
| `location point` | MySQL 空间类型不便于前端对接地图 SDK |
| `user_id` | 应改为 `member_id` |
| `device varchar(10)` | 太短 |
| `remark varchar(20)` | 太短 |
| 缺少唯一约束 | 同一活动同一人应只能签到一次 |

**建议修改：**

```sql
CREATE TABLE tb_sign_record (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    activity_id BIGINT       NOT NULL              COMMENT '活动ID',
    member_id   BIGINT       NOT NULL              COMMENT '成员ID',
    sign_time   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '签到时间',
    sign_type   VARCHAR(30)  DEFAULT 'NORMAL'      COMMENT '签到类型 NORMAL/LATE/LEAVE',
    device      VARCHAR(100)                       COMMENT '签到设备',
    longitude   DECIMAL(10,7)                      COMMENT '签到经度',
    latitude    DECIMAL(10,7)                      COMMENT '签到纬度',
    remark      VARCHAR(255)                       COMMENT '备注',
    FOREIGN KEY (activity_id) REFERENCES tb_activity(id),
    FOREIGN KEY (member_id)   REFERENCES tb_member(id),
    UNIQUE KEY uk_activity_member_sign (activity_id, member_id)
) COMMENT '签到记录表';
```

---

#### 3.11 `tb_study.sql` + `tb_study_record.sql` → 合并重构

**当前问题：**

| 问题 | 详情 |
|---|---|
| `tb_study.type varchar(6)` | "线上课程" = 12 字节，超限 |
| `study_time time` | TIME 是时刻不是时长，应用 `DECIMAL(5,1)` 存学时 |
| 缺少课程起止时间 | 无 `start_time / end_time` |
| 缺少状态和证书 | 无 `status`、`certificate_url` |

**建议：** 合并为党校学习记录表：

```sql
CREATE TABLE tb_party_school_record (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    member_id       BIGINT       NOT NULL              COMMENT '成员ID',
    course_name     VARCHAR(200) NOT NULL              COMMENT '课程名称',
    course_type     VARCHAR(30)                        COMMENT '课程类型 线上/线下/实践',
    start_time      DATETIME                           COMMENT '开始时间',
    end_time        DATETIME                           COMMENT '结束时间',
    study_hours     DECIMAL(5,1)                       COMMENT '学时',
    score           DECIMAL(5,1)                       COMMENT '成绩',
    status          VARCHAR(20)  DEFAULT 'ENROLLED'    COMMENT '状态 ENROLLED/COMPLETED/FAILED',
    certificate_url VARCHAR(500)                       COMMENT '结业证书URL',
    remark          VARCHAR(500)                       COMMENT '备注',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (member_id) REFERENCES tb_member(id)
) COMMENT '党校学习记录表';
```

> ✅ 可删除：`tb_study.sql`、`tb_study_record.sql`

---

#### 3.12 `tb_notice.sql` + `tb_notice_top.sql` → 合并

**当前问题：** 置顶逻辑拆成独立表属过度设计；`status` 用数字 1/2/3 含义不明；缺少 `expire_time`。

**建议：** 合并为一张表：

```sql
CREATE TABLE tb_notice (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '公告ID',
    title        VARCHAR(200) NOT NULL              COMMENT '公告标题',
    content      TEXT         NOT NULL              COMMENT '公告内容',
    publisher_id BIGINT                             COMMENT '发布人(用户ID)',
    top_flag     TINYINT      DEFAULT 0             COMMENT '置顶标志 1置顶/0不置顶',
    top_role_id  BIGINT       DEFAULT 0             COMMENT '置顶目标角色(0=全部)',
    status       VARCHAR(20)  DEFAULT 'PUBLISHED'   COMMENT '状态 DRAFT/PUBLISHED/EXPIRED',
    publish_time DATETIME                           COMMENT '发布时间',
    expire_time  DATETIME                           COMMENT '过期时间',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (publisher_id) REFERENCES tb_user(id),
    FOREIGN KEY (top_role_id) REFERENCES tb_role(id)
) COMMENT '公告表';
```

> ✅ 可删除：`tb_notice_top.sql`

---

### 🟢 低优先级（规范统一 + 删除冗余）

---

#### 3.13 `tb_user_detail.sql` → 并入 tb_user

`tb_user` 和 `tb_user_detail` 是 1:1 关系，拆分无意义，增加 JOIN 开销。建议：

```sql
-- 删除 tb_user_detail，将字段并入 tb_user
ALTER TABLE tb_user ADD COLUMN student_id           VARCHAR(30)  COMMENT '学号';
ALTER TABLE tb_user ADD COLUMN identity_card_number VARCHAR(18)  COMMENT '身份证号';
ALTER TABLE tb_user ADD COLUMN avatar               VARCHAR(255) COMMENT '头像URL';
```

> ✅ 可删除：`tb_user_detail.sql`

---

#### 3.14 `tb_user_permission.sql` → 删除

与 `tb_role_permission` 功能完全相同（两张表都是 role_id + permission_name），且存在拼写错误 `permission_nmae`。

> ✅ 可删除：`tb_user_permission.sql`

---

#### 3.15 `tb_identity_change.sql` — 关联修正

**问题：** `user_id` 应改为 `member_id`，缺少 `operator_id` 外键。

```sql
CREATE TABLE tb_identity_change_log (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    member_id      BIGINT       NOT NULL              COMMENT '成员ID',
    from_identity  BIGINT                             COMMENT '变更前身份ID',
    to_identity    BIGINT       NOT NULL              COMMENT '变更后身份ID',
    operator_id    BIGINT       NOT NULL              COMMENT '操作人(用户ID)',
    reason         VARCHAR(500)                       COMMENT '变更原因',
    create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
    FOREIGN KEY (member_id)     REFERENCES tb_member(id),
    FOREIGN KEY (from_identity) REFERENCES tb_identity(id),
    FOREIGN KEY (to_identity)   REFERENCES tb_identity(id),
    FOREIGN KEY (operator_id)   REFERENCES tb_user(id)
) COMMENT '身份变更记录表（党员发展轨迹）';
```

---

#### 3.16 `tb_role_permission.sql` + `tb_user_role.sql` — 类型统一

将 `id` 从 `int` 改为 `BIGINT`，所有外键对应改为 `BIGINT`。`tb_role_permission` 中 `permission_name` 应改为 `permission_id` 并外键关联 `tb_permission`。

---

## 四、全局性问题汇总

| # | 问题 | 涉及表 | 建议 |
|---|---|---|---|
| 1 | 主键全部用 `int` | 所有表 | 统一改为 `BIGINT`（int 上限 ~21 亿，大数据量场景有风险） |
| 2 | 时间字段混用 `TIMESTAMP` | 多数表 | 统一改为 `DATETIME`（TIMESTAMP 有 2038 年溢出问题） |
| 3 | 枚举全部用 `tinyint` 数字 | 多数表 | 统一改为 `VARCHAR` 语义值 + `CHECK` 约束 |
| 4 | 字段长度普遍偏小 | 多数表 | `varchar(10)` 类字段全部按业务放大 |
| 5 | 坐标用 MySQL 空间类型 | tb_sign_record, tb_activity_detail | 改为 `DECIMAL(10,7)` 经纬度，更通用 |
| 6 | 密码用 MD5 | tb_user | 改为 BCrypt，字段扩至 `varchar(255)` |
| 7 | 权限体系冗余 | tb_user_permission, tb_role_permission | 建立标准 RBAC 三角：用户→角色→权限 |
| 8 | 政治身份与系统角色耦合 | tb_identity | 删除 `role_id` 外键，解耦两个概念 |
| 9 | 拼写错误 | tb_user_permission | `permission_nmae` → `permission_name` |

---

## 五、字段长度对比速查表

| 表 | 字段 | tb*.sql 当前 | databases.sql 目标 | 说明 |
|---|---|---|---|---|
| tb_user | username | `varchar(10)` | `varchar(50)` | 学号通常 10-15 位 |
| tb_user | password | `varchar(64)` | `varchar(255)` | BCrypt 需 ≥ 60 |
| tb_role | role_name | `varchar(10)` | `varchar(50)` | — |
| tb_identity | identity_name | `varchar(10)` | `varchar(50)` | "入党申请人"=15字节 |
| tb_branch | branch_name | `varchar(20)` | `varchar(100)` | 支部全称 |
| tb_branch | college | `varchar(10)` | `varchar(100)` | 学院全称 |
| tb_news | title | `varchar(10)` | `varchar(200)` | 新闻标题 |
| tb_activity | title | `varchar(20)` | `varchar(200)` | 活动标题 |
| tb_activity | description | `varchar(30)` | `TEXT` | 活动简介 |
| tb_thought_report | title | `varchar(10)` | `varchar(200)` | 思想汇报标题 |
| tb_thought_report | file | `varchar(50)` | `varchar(500)` | MinIO URL |
| tb_operation | ip_address | `varchar(10)` | `varchar(50)` | IPv6 |
| tb_operation | user_agent | `varchar(10)` | `varchar(500)` | 完整 UA |
| tb_study | type | `varchar(6)` | `varchar(30)` | 课程类型 |

---

## 六、建议操作清单

| 优先级 | 操作 | 涉及文件 |
|---|---|---|
| 🔴 | 修改字段类型和长度 | tb_user, tb_role, tb_identity, tb_news, tb_thought_report, tb_operation, tb_branch, tb_activity, tb_activity_type, tb_sign_record, tb_study, tb_study_record, tb_notice |
| 🔴 | 添加缺失关键字段 | tb_user, tb_role, tb_news, tb_thought_report, tb_operation, tb_branch, tb_activity_type, tb_notice |
| 🔴 | 修复密码安全（MD5→BCrypt） | tb_user |
| 🟡 | 合并冗余表 → 删除源文件 | tb_activity_detail + tb_activity_identity → 并入 tb_activity |
| 🟡 | 合并冗余表 → 删除源文件 | tb_study + tb_study_record → tb_party_school_record |
| 🟡 | 合并冗余表 → 删除源文件 | tb_notice_top → 并入 tb_notice |
| 🟡 | 合并冗余表 → 删除源文件 | tb_user_detail → 并入 tb_user |
| 🟡 | 删除重复表 | tb_user_permission（与 tb_role_permission 功能重复） |
| 🟡 | 修复关联字段错误 | tb_identity_change：user_id → member_id |
| 🟡 | 解耦不当关联 | tb_identity：删除 role_id 外键 |
| 🟢 | 添加缺失的新表 | tb_permission, tb_member, tb_cultivation_contact, tb_cultivation_material, tb_activity_member, tb_resource, tb_dict |
| 🟢 | 全局类型统一 | int → BIGINT, timestamp → DATETIME, 数字枚举 → VARCHAR 语义值 |

---

## 七、目标 ER 关系概要（databases.sql 设计）

```
┌─────────────┐     ┌──────────────┐     ┌──────────────────┐
│  tb_user    │────→│ tb_user_role │←────│    tb_role       │
│  (用户表)    │     │ (用户-角色)   │     │   (角色表)        │
└─────────────┘     └──────────────┘     └──────────────────┘
       │                                        │
       │                               ┌────────┴──────────┐
       │                               │  tb_role_permission│
       │                               │  (角色-权限)        │
       │                               └────────┬──────────┘
       │                                        │
       │                               ┌────────┴──────────┐
       │                               │  tb_permission     │
       │                               │  (权限字典)         │
       │                               └────────────────────┘
       │
       │    ┌──────────────────────────────────────────────┐
       │    │              模块二：组织体系                    │
       │    │  tb_branch (党支部) ── secretary_id → tb_user │
       │    └──────────────────────────────────────────────┘
       │
       │    ┌──────────────────────────────────────────────┐
       │    │              模块三：成员档案体系                │
       │    │  tb_member ── branch_id  → tb_branch         │
       │    │  tb_member ── identity_id → tb_identity      │
       │    │  tb_identity_change_log (身份变更轨迹)         │
       │    │  tb_cultivation_contact (培养联系人)           │
       │    │  tb_thought_report (思想汇报)                  │
       │    │  tb_party_school_record (党校学习)             │
       │    │  tb_cultivation_material (培养材料/MinIO)      │
       │    └──────────────────────────────────────────────┘
       │
       │    ┌──────────────────────────────────────────────┐
       │    │              模块四：活动业务体系                │
       │    │  tb_activity_type (活动类型字典)               │
       │    │  tb_activity (活动)                            │
       │    │  tb_activity_member (活动参与)                 │
       │    │  tb_sign_record (签到记录)                     │
       │    └──────────────────────────────────────────────┘
       │
       │    ┌──────────────────────────────────────────────┐
       │    │              模块五：内容管理体系                │
       │    │  tb_news (新闻)                                │
       │    │  tb_notice (公告)                              │
       │    │  tb_resource (资源下载)                         │
       │    └──────────────────────────────────────────────┘
       │
       └────┌──────────────────────────────────────────────┐
            │              模块六：系统辅助                    │
            │  tb_operation_log (操作审计)                    │
            │  tb_dict (数据字典)                            │
            └──────────────────────────────────────────────┘
```
