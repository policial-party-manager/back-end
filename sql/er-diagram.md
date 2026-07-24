# 党建云平台 — 数据库 ER 图

```mermaid
erDiagram
    tb_user {
        bigint id PK "用户ID"
        varchar username UK "账号"
        varchar password "密码(BCrypt)"
        varchar real_name "姓名"
        varchar phone "手机号"
        varchar email "邮箱"
        tinyint status "状态 1启用/0停用"
        datetime create_time "创建时间"
        datetime update_time "更新时间"
    }

    tb_role {
        bigint id PK "角色ID"
        varchar role_name "角色名称"
        varchar role_code UK "角色编码 super_admin/branch_admin/student"
        varchar description "描述"
    }

    tb_permission {
        bigint id PK "权限ID"
        varchar permission_name "权限名称"
        varchar permission_code UK "权限编码"
        varchar description "描述"
    }

    tb_user_role {
        bigint id PK "关联ID"
        bigint user_id FK "用户ID"
        bigint role_id FK "角色ID"
    }

    tb_role_permission {
        bigint id PK "关联ID"
        bigint role_id FK "角色ID"
        bigint permission_id FK "权限ID"
    }

    tb_branch {
        bigint id PK "支部ID"
        varchar branch_name "支部名称"
        varchar college "所属学院"
        bigint secretary_id FK "支部书记(用户ID)"
        text description "支部简介"
        tinyint status "状态"
        datetime create_time "创建时间"
    }

    tb_identity {
        bigint id PK "身份ID"
        varchar identity_name "身份名称"
        int level "发展阶段 1-6"
        varchar description "描述"
    }

    tb_member {
        varchar student_no PK "学号"
        varchar name "姓名"
        varchar gender "性别"
        varchar college "学院"
        varchar grade "年级"
        varchar major "专业"
        varchar class_name "班级"
        bigint branch_id FK "所属支部"
        bigint identity_id FK "政治身份"
        varchar phone "手机号"
        varchar email "邮箱"
        varchar avatar "头像URL"
        tinyint status "状态"
        varchar remark "备注"
        datetime create_time "创建时间"
    }

    tb_activity {
        bigint id PK "活动ID"
        varchar title "活动标题"
        varchar type "活动类型"
        text description "活动描述"
        bigint branch_id FK "主办支部"
        bigint creator_id FK "创建人(用户ID)"
        varchar location "活动地点"
        datetime start_time "开始时间"
        datetime end_time "结束时间"
        datetime sign_start "签到开始"
        datetime sign_end "签到结束"
        varchar target_identity "目标身份"
        varchar status "状态 DRAFT/ACTIVE/FINISHED"
        datetime create_time "创建时间"
    }

    tb_activity_member {
        bigint id PK "关联ID"
        bigint activity_id FK "活动ID"
        varchar member_id FK "成员学号"
        datetime create_time "创建时间"
    }

    tb_sign_record {
        bigint id PK "记录ID"
        bigint activity_id FK "活动ID"
        varchar member_id FK "成员学号"
        datetime sign_time "签到时间"
        varchar sign_type "签到类型 NORMAL/LATE/LEAVE"
        varchar device "签到设备"
        varchar remark "备注"
    }

    %% ===== 用户与权限 =====
    tb_user ||--o{ tb_user_role : "用户角色"
    tb_role ||--o{ tb_user_role : "角色分配"
    tb_role ||--o{ tb_role_permission : "角色权限"
    tb_permission ||--o{ tb_role_permission : "权限授予"

    %% ===== 党支部 =====
    tb_user ||--o{ tb_branch : "担任书记"
    tb_branch ||--o{ tb_member : "支部成员"

    %% ===== 政治身份 =====
    tb_identity ||--o{ tb_member : "身份归类"

    %% ===== 活动 =====
    tb_branch ||--o{ tb_activity : "主办活动"
    tb_user ||--o{ tb_activity : "创建活动"
    tb_activity ||--o{ tb_activity_member : "活动报名"
    tb_member ||--o{ tb_activity_member : "参与活动"

    %% ===== 签到 =====
    tb_activity ||--o{ tb_sign_record : "签到记录"
    tb_member ||--o{ tb_sign_record : "成员签到"
```

## 表清单

| # | 表名 | 说明 | Java 实体 |
|---|------|------|-----------|
| 1 | `tb_user` | 用户账号 | `User.java` |
| 2 | `tb_role` | 系统角色 | `Role.java` |
| 3 | `tb_permission` | 权限定义 | `Permission.java` |
| 4 | `tb_user_role` | 用户-角色关联 | `UserRole.java` |
| 5 | `tb_role_permission` | 角色-权限关联 | `RolePermission.java` |
| 6 | `tb_branch` | 党支部 | `Branch.java` |
| 7 | `tb_identity` | 政治身份 | `Identity.java` |
| 8 | `tb_member` | 成员信息 | `Member.java` |
| 9 | `tb_activity` | 党建活动 | *(待实现)* |
| 10 | `tb_activity_member` | 活动-成员关联 | *(待实现)* |
| 11 | `tb_sign_record` | 签到记录 | *(待实现)* |

> 在 VS Code 中安装 **Markdown Preview Mermaid Support** 插件即可直接预览 ER 图。
> 也可以复制 mermaid 代码到 [mermaid.live](https://mermaid.live) 导出 PNG/SVG。
