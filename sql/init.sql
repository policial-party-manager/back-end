-- =============================================
-- 党建云平台 - 权限管理 初始化脚本
-- =============================================

CREATE DATABASE IF NOT EXISTS party_building DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE party_building;

-- 党支部表
DROP TABLE IF EXISTS tb_branch;
CREATE TABLE tb_branch (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    branch_name VARCHAR(64)  NOT NULL COMMENT '党支部名称',
    college     VARCHAR(64)  COMMENT '所属学院',
    secretary   VARCHAR(32)  COMMENT '支部书记',
    description VARCHAR(255) COMMENT '描述',
    status      TINYINT      DEFAULT 1 COMMENT '状态 1正常 0停用',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='党支部';

-- 用户登录表
DROP TABLE IF EXISTS tb_user;
CREATE TABLE tb_user (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(32)  NOT NULL UNIQUE COMMENT '用户名(学号/工号)',
    password    VARCHAR(128) NOT NULL COMMENT '密码(BCrypt)',
    real_name   VARCHAR(32)  NOT NULL COMMENT '真实姓名',
    role        VARCHAR(20)  NOT NULL DEFAULT 'student' COMMENT '角色: super_admin/branch_admin/student',
    branch_id   BIGINT       COMMENT '所属党支部ID(支部管理员必填)',
    status      TINYINT      DEFAULT 1 COMMENT '状态 1正常 0停用',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_branch (branch_id)
) ENGINE=InnoDB COMMENT='用户登录表';

-- 成员信息表（核心表）
DROP TABLE IF EXISTS tb_member;
CREATE TABLE tb_member (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_no  VARCHAR(20)  NOT NULL UNIQUE COMMENT '学号',
    name        VARCHAR(32)  NOT NULL COMMENT '姓名',
    gender      VARCHAR(4)   COMMENT '性别',
    college     VARCHAR(64)  COMMENT '学院',
    grade       VARCHAR(10)  COMMENT '年级 如2024级',
    major       VARCHAR(64)  COMMENT '专业',
    class_name  VARCHAR(32)  COMMENT '班级',
    branch_id   BIGINT       COMMENT '所属党支部ID',
    identity    VARCHAR(20)  DEFAULT 'ordinary' COMMENT '党员发展阶段',
    phone       VARCHAR(20)  COMMENT '手机号',
    email       VARCHAR(64)  COMMENT '邮箱',
    status      TINYINT      DEFAULT 1 COMMENT '状态 1正常 0停用',
    remark      VARCHAR(255) COMMENT '备注',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_branch (branch_id),
    INDEX idx_identity (identity),
    INDEX idx_college (college)
) ENGINE=InnoDB COMMENT='成员信息表';

-- =============================================
-- 种子数据
-- =============================================

INSERT INTO tb_branch (id, branch_name, college) VALUES
(1, '第一党支部', '信息工程学院'),
(2, '第二党支部', '信息工程学院'),
(3, '研究生党支部', '信息工程学院');

INSERT INTO tb_member (id, student_no, name, gender, college, grade, major, class_name, branch_id, identity, phone) VALUES
(1, '20240001', '王同学', '男', '信息工程学院', '2024级', '计算机科学与技术', '计算机202401', 1, 'ordinary',   '13800000001'),
(2, '20240002', '李同学', '女', '信息工程学院', '2024级', '计算机科学与技术', '计算机202401', 1, 'activist',   '13800000002'),
(3, '20240003', '张同学', '男', '信息工程学院', '2024级', '数据科学与大数据', '大数据202401', 2, 'development','13800000003'),
(4, '20240004', '赵同学', '女', '信息工程学院', '2023级', '计算机科学与技术', '计算机202301', 1, 'probationary','13800000004'),
(5, '20240005', '陈同学', '男', '信息工程学院', '2023级', '数据科学与大数据', '大数据202301', 2, 'full',       '13800000005');

-- 用户数据由 DataInitializer 在应用启动时自动创建(含BCrypt加密密码)
-- 默认账号: admin/123456  branch1/123456  student1/123456
