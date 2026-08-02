create table tb_user
(
    id              int auto_increment comment '用户id'
        primary key,
    username        varchar(10) default 'no_name'               not null comment '用户名',
    password        varchar(64) default (md5(_utf8mb4'123456')) not null comment '密码',
    create_time     timestamp   default CURRENT_TIMESTAMP       not null comment '用户创建时间',
    update_time     timestamp                                   not null on update CURRENT_TIMESTAMP comment '最后一次更新',
    last_login_time timestamp   default CURRENT_TIMESTAMP       not null comment '最近一次登录',
    status          tinyint     default 1                       not null comment '用户状态
1 启用
2 停用',
    constraint tb_user_pk
        unique (username),
    constraint chk_status
        check (`status` in (1, 2))
);

create index tb_user_status_index
    on tb_user (status);

create index tb_user_username_index
    on tb_user (username);

