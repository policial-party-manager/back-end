-- =============================================================
-- 党建云平台 — 党员发展全过程管理系统
-- 数据库建表脚本 (MySQL 8.0+)
-- =============================================================

CREATE DATABASE IF NOT EXISTS party_manager
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_general_ci;

USE party_manager;


-- =============================================================
-- 模块一：用户权限体系 (RBAC)
-- =============================================================

-- 用户表（支持 SSO + 本地账号）
CREATE TABLE tb_user (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT  COMMENT '用户ID',
    user_no     VARCHAR(50)  NOT NULL UNIQUE       COMMENT '统一身份号（学号/工号）',
    username    VARCHAR(50)  NOT NULL UNIQUE       COMMENT '登录账号',
    password    VARCHAR(255)                       COMMENT '密码（SSO用户可为空）',
    real_name   VARCHAR(50)  NOT NULL              COMMENT '姓名',
    phone       VARCHAR(20)                        COMMENT '手机号',
    email       VARCHAR(100)                       COMMENT '邮箱',
    status      TINYINT      DEFAULT 1             COMMENT '状态 1启用/0停用',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP
                             ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT '用户表';

-- 角色表
CREATE TABLE tb_role (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT  COMMENT '角色ID',
    role_name   VARCHAR(50)  NOT NULL              COMMENT '角色名称',
    role_code   VARCHAR(50)  NOT NULL UNIQUE       COMMENT '角色编码 super_admin/branch_admin/student',
    description VARCHAR(255)                       COMMENT '角色描述'
) COMMENT '角色表';

-- 权限表
CREATE TABLE tb_permission (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT  COMMENT '权限ID',
    permission_name VARCHAR(50)                        COMMENT '权限名称',
    permission_code VARCHAR(100) UNIQUE                COMMENT '权限编码',
    description     VARCHAR(255)                       COMMENT '权限描述'
) COMMENT '权限表';

-- 用户-角色关联表
CREATE TABLE tb_user_role (
    id      BIGINT PRIMARY KEY AUTO_INCREMENT  COMMENT '关联ID',
    user_id BIGINT NOT NULL                    COMMENT '用户ID',
    role_id BIGINT NOT NULL                    COMMENT '角色ID',
    FOREIGN KEY (user_id) REFERENCES tb_user(id),
    FOREIGN KEY (role_id) REFERENCES tb_role(id),
    UNIQUE KEY uk_user_role (user_id, role_id)
) COMMENT '用户-角色关联表';

-- 角色-权限关联表
CREATE TABLE tb_role_permission (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT  COMMENT '关联ID',
    role_id       BIGINT NOT NULL                    COMMENT '角色ID',
    permission_id BIGINT NOT NULL                    COMMENT '权限ID',
    FOREIGN KEY (role_id)       REFERENCES tb_role(id),
    FOREIGN KEY (permission_id) REFERENCES tb_permission(id),
    UNIQUE KEY uk_role_perm (role_id, permission_id)
) COMMENT '角色-权限关联表';


-- =============================================================
-- 模块二：组织体系
-- =============================================================

-- 党支部表
CREATE TABLE tb_branch (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT  COMMENT '支部ID',
    branch_name  VARCHAR(100) NOT NULL              COMMENT '支部名称',
    college      VARCHAR(100)                       COMMENT '所属学院',
    secretary_id BIGINT                             COMMENT '支部书记(用户ID)',
    description  TEXT                                COMMENT '支部简介',
    status       TINYINT      DEFAULT 1             COMMENT '状态 1启用/0停用',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP
                              ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (secretary_id) REFERENCES tb_user(id)
) COMMENT '党支部表';


-- =============================================================
-- 模块三：成员档案体系
-- =============================================================

-- 政治身份字典表
CREATE TABLE tb_identity (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT  COMMENT '身份ID',
    identity_name VARCHAR(50) NOT NULL               COMMENT '身份名称',
    level         INT                                 COMMENT '发展阶段 1普通学生/2入党申请人/3积极分子/4发展对象/5预备党员/6正式党员',
    description   VARCHAR(255)                        COMMENT '描述'
) COMMENT '政治身份字典表';

-- 成员信息表（核心表）
CREATE TABLE tb_member (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT  COMMENT '成员ID',
    student_no  VARCHAR(30)  NOT NULL UNIQUE       COMMENT '学号',
    name        VARCHAR(50)  NOT NULL              COMMENT '姓名',
    gender      VARCHAR(10)                        COMMENT '性别',
    college     VARCHAR(100)                       COMMENT '学院',
    grade       VARCHAR(20)                        COMMENT '年级',
    major       VARCHAR(100)                       COMMENT '专业',
    class_name  VARCHAR(100)                       COMMENT '班级',
    branch_id   BIGINT                             COMMENT '所属党支部',
    identity_id BIGINT                             COMMENT '当前政治身份',
    phone       VARCHAR(20)                        COMMENT '手机号',
    email       VARCHAR(100)                       COMMENT '邮箱',
    avatar      VARCHAR(255)                       COMMENT '头像URL',
    status      TINYINT      DEFAULT 1             COMMENT '状态 1正常/0停用',
    remark      VARCHAR(500)                       COMMENT '备注',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP
                             ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (branch_id)   REFERENCES tb_branch(id),
    FOREIGN KEY (identity_id) REFERENCES tb_identity(id)
) COMMENT '成员信息表';

-- 身份变更记录表（框架重点要求：完整保留发展轨迹）
CREATE TABLE tb_identity_change_log (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT  COMMENT '记录ID',
    member_id      BIGINT        NOT NULL             COMMENT '成员ID',
    from_identity  BIGINT                             COMMENT '变更前身份ID',
    to_identity    BIGINT        NOT NULL             COMMENT '变更后身份ID',
    operator_id    BIGINT        NOT NULL             COMMENT '操作人(用户ID)',
    reason         VARCHAR(500)                       COMMENT '变更原因',
    create_time    DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
    FOREIGN KEY (member_id)      REFERENCES tb_member(id),
    FOREIGN KEY (from_identity)  REFERENCES tb_identity(id),
    FOREIGN KEY (to_identity)    REFERENCES tb_identity(id),
    FOREIGN KEY (operator_id)    REFERENCES tb_user(id)
) COMMENT '身份变更记录表（党员发展轨迹）';

-- 培养联系人表
CREATE TABLE tb_cultivation_contact (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT  COMMENT '关联ID',
    member_id   BIGINT NOT NULL                    COMMENT '成员ID',
    contact_id  BIGINT NOT NULL                    COMMENT '联系人(用户ID)',
    FOREIGN KEY (member_id)  REFERENCES tb_member(id),
    FOREIGN KEY (contact_id) REFERENCES tb_user(id),
    UNIQUE KEY uk_member_contact (member_id, contact_id)
) COMMENT '培养联系人表';

-- 思想汇报表
CREATE TABLE tb_thought_report (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT  COMMENT '汇报ID',
    member_id     BIGINT        NOT NULL             COMMENT '成员ID',
    title         VARCHAR(200)  NOT NULL             COMMENT '汇报标题',
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

-- 党校学习记录表
CREATE TABLE tb_party_school_record (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT  COMMENT '记录ID',
    member_id     BIGINT        NOT NULL             COMMENT '成员ID',
    course_name   VARCHAR(200)  NOT NULL             COMMENT '课程名称',
    start_time    DATETIME                           COMMENT '开始时间',
    end_time      DATETIME                           COMMENT '结束时间',
    study_hours   DECIMAL(5,1)                       COMMENT '学时',
    score         DECIMAL(5,1)                       COMMENT '成绩',
    status        VARCHAR(20)  DEFAULT 'ENROLLED'    COMMENT '状态 ENROLLED/COMPLETED/FAILED',
    certificate_url VARCHAR(500)                     COMMENT '结业证书URL',
    remark        VARCHAR(500)                       COMMENT '备注',
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (member_id) REFERENCES tb_member(id)
) COMMENT '党校学习记录表';

-- 培养材料表
CREATE TABLE tb_cultivation_material (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT  COMMENT '材料ID',
    member_id   BIGINT        NOT NULL             COMMENT '成员ID',
    title       VARCHAR(200)  NOT NULL             COMMENT '材料名称',
    category    VARCHAR(50)                        COMMENT '材料分类 申请书/思想汇报/考察表/转正申请/其他',
    file_name   VARCHAR(255)                       COMMENT '文件名',
    file_url    VARCHAR(500)  NOT NULL             COMMENT '文件URL（MinIO）',
    file_size   BIGINT                             COMMENT '文件大小(字节)',
    upload_user BIGINT        NOT NULL             COMMENT '上传人(用户ID)',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    FOREIGN KEY (member_id)   REFERENCES tb_member(id),
    FOREIGN KEY (upload_user) REFERENCES tb_user(id)
) COMMENT '培养材料表';


-- =============================================================
-- 模块四：活动业务体系
-- =============================================================

-- 活动类型字典表
CREATE TABLE tb_activity_type (
    id      BIGINT PRIMARY KEY AUTO_INCREMENT  COMMENT '类型ID',
    name    VARCHAR(50) NOT NULL               COMMENT '类型名称 组织生活/主题党日/党课学习/二课活动/志愿服务/其他',
    code    VARCHAR(50) NOT NULL UNIQUE        COMMENT '类型编码',
    need_sign     TINYINT DEFAULT 1            COMMENT '是否需要签到',
    count_to_file TINYINT DEFAULT 1            COMMENT '是否计入培养档案'
) COMMENT '活动类型字典表';

-- 活动表
CREATE TABLE tb_activity (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT  COMMENT '活动ID',
    title           VARCHAR(200) NOT NULL              COMMENT '活动名称',
    type_id         BIGINT                             COMMENT '活动类型ID',
    description     TEXT                                COMMENT '活动简介',
    cover_url       VARCHAR(500)                       COMMENT '活动封面URL',
    branch_id       BIGINT                             COMMENT '主办支部',
    creator_id      BIGINT                             COMMENT '创建人(用户ID)',
    location        VARCHAR(200)                       COMMENT '活动地点',
    start_time      DATETIME                           COMMENT '活动开始时间',
    end_time        DATETIME                           COMMENT '活动结束时间',
    sign_start      DATETIME                           COMMENT '签到开始时间',
    sign_end        DATETIME                           COMMENT '签到截止时间',
    target_identity VARCHAR(100)                       COMMENT '目标身份（逗号分隔的identity_id）',
    max_participants INT                               COMMENT '人数上限（NULL=不限）',
    status          VARCHAR(30) DEFAULT 'DRAFT'        COMMENT '状态 DRAFT/PUBLISHED/IN_PROGRESS/FINISHED/ARCHIVED',
    create_time     DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME    DEFAULT CURRENT_TIMESTAMP
                                ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (type_id)    REFERENCES tb_activity_type(id),
    FOREIGN KEY (branch_id)  REFERENCES tb_branch(id),
    FOREIGN KEY (creator_id) REFERENCES tb_user(id)
) COMMENT '活动表';

-- 活动参与表（报名 + 参与记录）
CREATE TABLE tb_activity_member (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT  COMMENT '关联ID',
    activity_id BIGINT   NOT NULL                  COMMENT '活动ID',
    member_id   BIGINT   NOT NULL                  COMMENT '成员ID',
    status      VARCHAR(20) DEFAULT 'REGISTERED'   COMMENT '状态 REGISTERED/SIGNED/LEAVE/ABSENT',
    create_time DATETIME  DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
    FOREIGN KEY (activity_id) REFERENCES tb_activity(id),
    FOREIGN KEY (member_id)   REFERENCES tb_member(id),
    UNIQUE KEY uk_activity_member (activity_id, member_id)
) COMMENT '活动参与表';

-- 签到记录表
CREATE TABLE tb_sign_record (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT  COMMENT '记录ID',
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


-- =============================================================
-- 模块五：内容管理体系
-- =============================================================

-- 新闻表（党建新闻 / 党建成果）
CREATE TABLE tb_news (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT  COMMENT '新闻ID',
    title        VARCHAR(200) NOT NULL              COMMENT '标题',
    cover_url    VARCHAR(500)                       COMMENT '封面图URL',
    summary      VARCHAR(500)                       COMMENT '摘要',
    content      LONGTEXT                           COMMENT '正文（富文本）',
    category     VARCHAR(30) DEFAULT 'NEWS'         COMMENT '分类 NEWS/ACHIEVEMENT',
    author       VARCHAR(50)                        COMMENT '作者',
    publisher_id BIGINT                             COMMENT '发布人(用户ID)',
    view_count   INT        DEFAULT 0               COMMENT '浏览次数',
    status       VARCHAR(20) DEFAULT 'DRAFT'        COMMENT '状态 DRAFT/PUBLISHED',
    publish_time DATETIME                           COMMENT '发布时间',
    create_time  DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME    DEFAULT CURRENT_TIMESTAMP
                             ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (publisher_id) REFERENCES tb_user(id)
) COMMENT '新闻表';

-- 公告表
CREATE TABLE tb_notice (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT  COMMENT '公告ID',
    title        VARCHAR(200) NOT NULL              COMMENT '公告标题',
    content      TEXT         NOT NULL              COMMENT '公告内容',
    publisher_id BIGINT                             COMMENT '发布人(用户ID)',
    top_flag     TINYINT      DEFAULT 0             COMMENT '置顶标志 1置顶/0不置顶',
    status       VARCHAR(20)  DEFAULT 'PUBLISHED'   COMMENT '状态 DRAFT/PUBLISHED/EXPIRED',
    publish_time DATETIME                           COMMENT '发布时间',
    expire_time  DATETIME                           COMMENT '过期时间',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP
                              ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (publisher_id) REFERENCES tb_user(id)
) COMMENT '公告表';

-- 资源下载表
CREATE TABLE tb_resource (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT  COMMENT '资源ID',
    title          VARCHAR(200) NOT NULL              COMMENT '资源名称',
    category       VARCHAR(50)                        COMMENT '分类 申请书模板/思想汇报模板/学习资料/常用表格/其他',
    file_name      VARCHAR(255) NOT NULL              COMMENT '文件名',
    file_url       VARCHAR(500) NOT NULL              COMMENT '文件URL（MinIO）',
    file_size      BIGINT                             COMMENT '文件大小(字节)',
    download_count INT          DEFAULT 0             COMMENT '下载次数',
    upload_user    BIGINT                             COMMENT '上传人(用户ID)',
    status         TINYINT      DEFAULT 1             COMMENT '状态 1正常/0下架',
    create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    update_time    DATETIME     DEFAULT CURRENT_TIMESTAMP
                                ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (upload_user) REFERENCES tb_user(id)
) COMMENT '资源下载表';


-- =============================================================
-- 模块六：系统辅助
-- =============================================================

-- 操作日志表
CREATE TABLE tb_operation_log (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT  COMMENT '日志ID',
    operator_id   BIGINT        NOT NULL             COMMENT '操作人(用户ID)',
    module        VARCHAR(50)                        COMMENT '操作模块',
    action        VARCHAR(50)   NOT NULL             COMMENT '操作类型 CREATE/UPDATE/DELETE/IMPORT/EXPORT',
    target_type   VARCHAR(50)                        COMMENT '操作对象类型',
    target_id     VARCHAR(50)                        COMMENT '操作对象ID',
    before_data   JSON                                COMMENT '修改前数据',
    after_data    JSON                                COMMENT '修改后数据',
    ip_address    VARCHAR(50)                        COMMENT '操作IP',
    user_agent    VARCHAR(500)                       COMMENT '浏览器UA',
    result        VARCHAR(20)  DEFAULT 'SUCCESS'     COMMENT '操作结果 SUCCESS/FAIL',
    error_msg     VARCHAR(1000)                      COMMENT '错误信息',
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    FOREIGN KEY (operator_id) REFERENCES tb_user(id)
) COMMENT '操作日志表';

-- 数据字典表
CREATE TABLE tb_dict (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT  COMMENT '字典ID',
    dict_type   VARCHAR(50)  NOT NULL              COMMENT '字典类型',
    dict_code   VARCHAR(50)  NOT NULL              COMMENT '字典编码',
    dict_value  VARCHAR(100) NOT NULL              COMMENT '字典值',
    sort_order  INT          DEFAULT 0             COMMENT '排序',
    status      TINYINT      DEFAULT 1             COMMENT '状态',
    remark      VARCHAR(255)                       COMMENT '备注',
    UNIQUE KEY uk_type_code (dict_type, dict_code)
) COMMENT '数据字典表';


-- =============================================================
-- 初始数据
-- =============================================================

-- 系统角色
INSERT INTO tb_role (role_code, role_name, description) VALUES
('super_admin',  '超级管理员', '全院党建管理，拥有所有权限'),
('branch_admin', '支部管理员', '管理所属党支部的成员和活动'),
('student',      '普通成员',   '查看培养信息、参与活动');

-- 政治身份
INSERT INTO tb_identity (identity_name, level, description) VALUES
('普通学生',   1, '非党员学生'),
('入党申请人', 2, '已提交入党申请书'),
('积极分子',   3, '已确定为入党积极分子'),
('发展对象',   4, '已确定为发展对象'),
('预备党员',   5, '已批准为预备党员'),
('正式党员',   6, '已转为正式党员');

-- 活动类型
INSERT INTO tb_activity_type (code, name, need_sign, count_to_file) VALUES
('org_life',      '组织生活会', 1, 1),
('theme_day',     '主题党日',   1, 1),
('party_lecture', '党课学习',   1, 1),
('second_class',  '二课活动',   1, 1),
('volunteer',     '志愿服务',   1, 1),
('training',      '培训活动',   1, 1),
('other',         '其他活动',   0, 0);

-- 测试用户（密码 123456 的 BCrypt 密文，生产环境请删除）
-- INSERT INTO tb_user (user_no, username, password, real_name) VALUES
-- ('T0001', 'admin',    '$2a$10$...', '张老师'),
-- ('T0002', 'branch1',  '$2a$10$...', '李支委'),
-- ('S0001', 'student1', '$2a$10$...', '王同学');
