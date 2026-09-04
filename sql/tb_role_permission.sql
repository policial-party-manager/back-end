create table tb_role_permission
(
    id              int auto_increment
        primary key,
    role_id         int         not null comment '角色id',
    permission_name varchar(20) not null comment '权限名称',
    constraint tb_role_permission_role_id_permission_name_uindex
        unique (role_id, permission_name),
    constraint tb_role_permission_tb_role_id_fk
        foreign key (role_id) references tb_role (id)
            on delete cascade
)
    comment '角色权限关联表';

��联表';

