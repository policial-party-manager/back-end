create table tb_notice_top
(
    id        int auto_increment
        primary key,
    notice_id int           not null comment '公告id',
    role_id   int default 0 not null comment '给哪些用户置顶(0为所有用户)',
    constraint tb_notice_top_tb_notice_id_fk
        foreign key (notice_id) references tb_notice (id)
            on update cascade on delete cascade,
    constraint tb_notice_top_tb_role_id_fk
        foreign key (role_id) references tb_role (id)
            on update set default on delete set default
)
    comment '公告置顶消息';

顶消息';

