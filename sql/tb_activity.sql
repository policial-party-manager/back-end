create table tb_activity
(
    id          int auto_increment
        primary key,
    branch_id   int                                   not null comment '支部id',
    title       varchar(20) default '无标题'          not null comment '标题',
    description varchar(30)                           not null comment '活动简介',
    type        int                                   null comment '活动类型',
    cover       varchar(100)                          null comment '封面图
file: 开头为本地图片
url: 开头为网络图片',
    start_time  timestamp                             not null comment '开始时间',
    end_time    timestamp                             not null comment '结束时间',
    create_time timestamp   default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time timestamp   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更改时间',
    constraint tb_activity_tb_activity_type_id_fk
        foreign key (type) references tb_activity_type (id),
    constraint tb_activity_tb_branch_id_fk
        foreign key (branch_id) references tb_branch (id)
            on delete cascade
)
    comment '活动表';

omment '活动表';

