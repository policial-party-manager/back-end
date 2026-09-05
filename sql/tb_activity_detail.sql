create table tb_activity_detail
(
    activity_id          int                            not null
        primary key,
    creator_id           int                            not null comment '创建人id',
    location             polygon                        null comment '地点(为null则为线上活动)',
    location_description varchar(500)                   null default null comment '地址(文字说明)',
    sign_start           datetime                       null comment '签到开始时间',
    sign_end             datetime                       null comment '签到结束时间',
    max_participants     int                            null comment '人数上限(为null则为无人数限制)',
    status               tinyint                        not null default 1 comment '活动状态',
    constraint tb_activity_detail_tb_activity_id_fk
        foreign key (activity_id) references tb_activity (id)
            on delete cascade,
    constraint tb_activity_detail_tb_user_id_fk
        foreign key (creator_id) references tb_user (id),
    constraint check_status
        check (`status` in (0, 1, 2))
)
    comment '活动详情';

