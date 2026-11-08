create table tb_activity_leave
(
    id          int auto_increment
        primary key,
    activity_id int                                  not null comment '活动id',
    user_id     int                                  not null comment '请假人id',
    reason      varchar(200)                         null comment '请假理由',
    operator_id int                                  not null comment '录入人id',
    status      tinyint    default 1                 not null comment '状态（1：生效，0：已撤销）',
    create_time timestamp  default CURRENT_TIMESTAMP not null comment '录入时间',
    update_time timestamp  default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint tb_activity_leave_activity_user_uindex
        unique (activity_id, user_id),
    constraint tb_activity_leave_tb_activity_id_fk
        foreign key (activity_id) references tb_activity (id),
    constraint tb_activity_leave_tb_user_id_fk
        foreign key (user_id) references tb_user (id),
    constraint tb_activity_leave_tb_operator_id_fk
        foreign key (operator_id) references tb_user (id)
)
    comment '活动请假（与签到记录是两个生命周期的实体：已批准的预期 vs 已发生的事实）';

create index tb_activity_leave_activity_id_index
    on tb_activity_leave (activity_id);

create index tb_activity_leave_user_id_index
    on tb_activity_leave (user_id);
