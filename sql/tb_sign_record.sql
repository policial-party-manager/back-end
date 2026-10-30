create table tb_sign_record
(
    id          int auto_increment
        primary key,
    activity_id int         not null comment '活动id',
    user_id     int         not null comment '用户id',
    sign_time   timestamp   not null comment '签到时间',
    sign_type   varchar(10) null comment '签到类型（sign=本人签到，makeup=补签）',
    device      varchar(10) null comment '签到设备（web/app/mini 等，可空）',
    location    point       null comment '地点（POINT，可空；读写按 WKT 字符串处理）',
    remark      varchar(20) null comment '备注',
    constraint tb_sign_record_activity_user_uindex
        unique (activity_id, user_id),
    constraint tb_sign_record_tb_activity_id_fk
        foreign key (activity_id) references tb_activity (id),
    constraint tb_sign_record_tb_user_id_fk
        foreign key (user_id) references tb_user (id)
)
    comment '签到记录';
