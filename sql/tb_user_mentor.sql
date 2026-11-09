create table tb_user_mentor
(
    id          int auto_increment
        primary key,
    user_id     int                                  not null comment '被培养人id（tb_user.id）',
    mentor_id   int                                  not null comment '培养联系人id（tb_user.id，须为党员）',
    status      tinyint    default 1                 not null comment '状态（1：在职，0：已移除）',
    create_time timestamp  default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time timestamp  default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint tb_user_mentor_user_mentor_uindex
        unique (user_id, mentor_id),
    constraint tb_user_mentor_tb_user_id_fk
        foreign key (user_id) references tb_user (id),
    constraint tb_user_mentor_tb_mentor_id_fk
        foreign key (mentor_id) references tb_user (id)
)
    comment '培养联系人（与 tb_user_detail.contact_person 紧急联系人无关）';

create index tb_user_mentor_user_id_index
    on tb_user_mentor (user_id);

create index tb_user_mentor_mentor_id_index
    on tb_user_mentor (mentor_id);
