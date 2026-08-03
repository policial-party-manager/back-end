create table tb_user_permission
(
    id              int auto_increment
        primary key,
    role_id         int         not null comment '角色id',
    permission_nmae varchar(20) not null comment '权限名称',
    constraint tb_user_permission_role_id_permission_nmae_uindex
        unique (role_id, permission_nmae),
    constraint tb_user_permission_tb_role_id_fk
        foreign key (role_id) references tb_role (id)
            on delete cascade
);

