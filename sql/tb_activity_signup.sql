create table tb_activity_signup
(
    id          int auto_increment
        primary key,
    activity_id int         not null comment '活动id',
    user_id     int         not null comment '用户id',
    signup_time timestamp default CURRENT_TIMESTAMP not null comment '报名时间',
    constraint tb_activity_signup_activity_user_uindex
        unique (activity_id, user_id),
    constraint tb_activity_signup_tb_activity_id_fk
        foreign key (activity_id) references tb_activity (id),
    constraint tb_activity_signup_tb_user_id_fk
        foreign key (user_id) references tb_user (id)
)
    comment '活动报名';
