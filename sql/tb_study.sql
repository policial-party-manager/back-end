create table tb_study
(
    id   int auto_increment
        primary key,
    name varchar(30) not null comment '课程名',
    type varchar(6)  not null comment '类型'
)
    comment '课程';

create index tb_study_type_index
    on tb_study (type);

