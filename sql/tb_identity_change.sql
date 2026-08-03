create table tb_identity_change
(
    id            int auto_increment
        primary key,
    user_id       int                                 not null comment '用户id',
    from_identity int                                 not null comment '从什么身份',
    to_identity   int                                 not null comment '到什么什么身份',
    operator_id   int                                 not null comment '操作者id',
    reason        text                                null comment '变更原因',
    create_time   timestamp default CURRENT_TIMESTAMP not null comment '变更时间',
    constraint tb_identity_change_tb_identity_id_fk
        foreign key (from_identity) references tb_identity (id),
    constraint tb_identity_change_tb_identity_id_fk_2
        foreign key (to_identity) references tb_identity (id),
    constraint tb_identity_change_tb_user_id_fk
        foreign key (user_id) references tb_user (id)
)
    comment '身份消息变更';

create index tb_identity_change_operator_id_index
    on tb_identity_change (operator_id);

