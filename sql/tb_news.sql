create table tb_news
(
    id          int auto_increment
        primary key,
    author      int               not null comment '作者',
    title       varchar(10)       not null comment '标题',
    cover       varchar(100)      null comment '封面',
    type        varchar(10)       null comment '类型',
    status      tinyint default 1 not null comment '状态',
    content     longtext          not null comment '正文',
    view_count  int     default 1 not null comment '浏览量',
    create_time timestamp         null comment '创建时间',
    update_time timestamp         null comment '修改时间',
    constraint tb_news_tb_user_id_fk
        foreign key (author) references tb_user (id),
    constraint checker
        check (`status` in (1, 2))
)
    comment '新闻';

create index tb_news_author_index
    on tb_news (author);

