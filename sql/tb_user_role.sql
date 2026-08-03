create table tb_user_role
(
    id      int auto_increment
        primary key,
    user_id int not null comment '用户id',
    role_id int null,
    constraint tb_user_role_user_id_role_id_uindex
        unique (user_id, role_id),
    constraint tb_user_role_tb_role_id_fk
        foreign key (role_id) references tb_role (id)
            on delete cascade,
    constraint tb_user_role_tb_user_id_fk
        foreign key (user_id) references tb_user (id)
            on delete cascade
)
    comment '用户角色关联表';

��关联表';

