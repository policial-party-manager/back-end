create table tb_operation
(
    id          int auto_increment
        primary key,
    operator_id int                                 not null comment '操作者id',
    ip_address  varchar(10)                         null comment '操作ip',
    operator    text                                null comment '操作',
    time        timestamp default CURRENT_TIMESTAMP not null comment '操作时间',
    user_agent  varchar(10)                         null comment '浏览器UA',
    result      text                                null comment '结果',
    constraint tb_operation_tb_user_id_fk
        foreign key (operator_id) references tb_user (id)
)
    comment '操作日志';

