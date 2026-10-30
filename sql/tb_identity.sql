create table tb_identity
(
    id            int auto_increment
        primary key,
    identity_name varchar(20) not null comment '身份名',
    level         int         null comment '发展阶段1-6',
    role_id       int         null comment '对应角色id（仅展示映射，不参与鉴权；身份变更永不联动登录角色）',
    description   text        null comment '说明',
    constraint tb_identity_identity_name_uindex
        unique (identity_name),
    constraint tb_identity_tb_role_id_fk
        foreign key (role_id) references tb_role (id)
)
    comment '政治身份';
