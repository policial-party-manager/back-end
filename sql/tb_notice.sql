create table tb_notice
(
    id           int auto_increment
        primary key,
    title        varchar(30) default '无标题'          not null comment '标题',
    content      text                                  not null comment '内容(以file:开头时识别为文件)',
    status       tinyint     default 1                 not null comment '状态
1 : 正常
2 : 已经删除
3 : 延期展示',
    publisher_id int         default 0                 not null comment '发布者的id ( 0为系统发布 )',
    publish_time timestamp   default (now())           not null,
    update_time  timestamp   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '修改时间',
    start_time   timestamp                             null comment '开始时间',
    end_time     timestamp                             null comment '结束时间',
    constraint tb_notice_tb_user_id_fk
        foreign key (publisher_id) references tb_user (id)
            on delete set default,
    constraint status_check
        check (`status` in (1, 2, 3))
)
    comment '公告表';

create index tb_notice_title_index
    on tb_notice (title);

