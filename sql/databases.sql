CREATE DATABASE IF NOT EXISTS party_manager
DEFAULT CHARACTER SET utf8mb4;


USE party_manager;



-- ==========================
-- 用户表
-- ==========================

CREATE TABLE tb_user(

id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',

username VARCHAR(50)
NOT NULL UNIQUE COMMENT '账号',

password VARCHAR(255)
NOT NULL COMMENT '密码',

real_name VARCHAR(50)
NOT NULL COMMENT '姓名',

phone VARCHAR(20),

email VARCHAR(100),

status TINYINT DEFAULT 1,

create_time DATETIME DEFAULT CURRENT_TIMESTAMP,

update_time DATETIME DEFAULT CURRENT_TIMESTAMP
ON UPDATE CURRENT_TIMESTAMP

);



-- ==========================
-- 角色表
-- ==========================

CREATE TABLE tb_role(

id BIGINT PRIMARY KEY AUTO_INCREMENT,

role_name VARCHAR(50)
NOT NULL,

role_code VARCHAR(50)
NOT NULL UNIQUE,

description VARCHAR(255)

);



-- ==========================
-- 权限表
-- ==========================

CREATE TABLE tb_permission(

id BIGINT PRIMARY KEY AUTO_INCREMENT,

permission_name VARCHAR(50),

permission_code VARCHAR(100)
UNIQUE,

description VARCHAR(255)

);



-- ==========================
-- 用户角色关系
-- ==========================

CREATE TABLE tb_user_role(

id BIGINT PRIMARY KEY AUTO_INCREMENT,

user_id BIGINT,

role_id BIGINT,


FOREIGN KEY(user_id)
REFERENCES tb_user(id),


FOREIGN KEY(role_id)
REFERENCES tb_role(id)

);



-- ==========================
-- 角色权限关系
-- ==========================

CREATE TABLE tb_role_permission(

id BIGINT PRIMARY KEY AUTO_INCREMENT,

role_id BIGINT,

permission_id BIGINT,


FOREIGN KEY(role_id)
REFERENCES tb_role(id),


FOREIGN KEY(permission_id)
REFERENCES tb_permission(id)

);



-- ==========================
-- 党支部表
-- ==========================

CREATE TABLE tb_branch(

id BIGINT PRIMARY KEY AUTO_INCREMENT,

branch_name VARCHAR(100)
NOT NULL,

college VARCHAR(100),

secretary_id BIGINT,

description TEXT,

status TINYINT DEFAULT 1,

create_time DATETIME DEFAULT CURRENT_TIMESTAMP

);



-- ==========================
-- 政治身份表
-- ==========================

CREATE TABLE tb_identity(

id BIGINT PRIMARY KEY AUTO_INCREMENT,

identity_name VARCHAR(50)
NOT NULL,

level INT,

description VARCHAR(255)

);



-- ==========================
-- 成员信息表
-- ==========================

CREATE TABLE tb_member(

student_no VARCHAR(30)
PRIMARY KEY COMMENT '学号',

name VARCHAR(50)
NOT NULL,

gender VARCHAR(10),

college VARCHAR(100),

grade VARCHAR(20),

major VARCHAR(100),

class_name VARCHAR(100),

branch_id BIGINT,

identity_id BIGINT,

phone VARCHAR(20),

email VARCHAR(100),

avatar VARCHAR(255),

status TINYINT DEFAULT 1,

remark VARCHAR(255),

create_time DATETIME DEFAULT CURRENT_TIMESTAMP,


FOREIGN KEY(branch_id)
REFERENCES tb_branch(id),


FOREIGN KEY(identity_id)
REFERENCES tb_identity(id)

);



-- ==========================
-- 活动表
-- ==========================

CREATE TABLE tb_activity(

id BIGINT PRIMARY KEY AUTO_INCREMENT,


title VARCHAR(100)
NOT NULL,


type VARCHAR(50),


description TEXT,


branch_id BIGINT,


creator_id BIGINT,


location VARCHAR(200),


start_time DATETIME,


end_time DATETIME,


sign_start DATETIME,


sign_end DATETIME,


target_identity VARCHAR(50),


status VARCHAR(30)
DEFAULT 'DRAFT',


create_time DATETIME DEFAULT CURRENT_TIMESTAMP,


FOREIGN KEY(branch_id)
REFERENCES tb_branch(id),


FOREIGN KEY(creator_id)
REFERENCES tb_user(id)

);



-- ==========================
-- 活动成员关系表
-- ==========================

CREATE TABLE tb_activity_member(

id BIGINT PRIMARY KEY AUTO_INCREMENT,


activity_id BIGINT,


member_id VARCHAR(30),


create_time DATETIME DEFAULT CURRENT_TIMESTAMP,


FOREIGN KEY(activity_id)
REFERENCES tb_activity(id),


FOREIGN KEY(member_id)
REFERENCES tb_member(student_no)

);



-- ==========================
-- 签到记录表
-- ==========================

CREATE TABLE tb_sign_record(

id BIGINT PRIMARY KEY AUTO_INCREMENT,


activity_id BIGINT,


member_id VARCHAR(30),


sign_time DATETIME DEFAULT CURRENT_TIMESTAMP,


sign_type VARCHAR(30)
DEFAULT 'NORMAL',


device VARCHAR(100),


remark VARCHAR(255),


FOREIGN KEY(activity_id)
REFERENCES tb_activity(id),


FOREIGN KEY(member_id)
REFERENCES tb_member(student_no)

);
