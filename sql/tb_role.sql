create table tb_role
(
    id          int auto_increment
        primary key,
    role_name   varchar(10) not null comment '角色名称',
    description varchar(20) null comment '角色描述',
    constraint chk_role_code_prefix
        check (`role_name` like _utf8mb4\'ROLE_%\')
);

