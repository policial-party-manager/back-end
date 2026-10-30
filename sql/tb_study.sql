create table tb_study
(
    id   int auto_increment
        primary key,
    name varchar(30) not null comment '课程名',
    type varchar(20) not null comment '类型（自由文本，如 必修/选修/专题）',
    constraint tb_study_name_uindex
        unique (name)
)
    comment '课程';

create index tb_study_type_index
    on tb_study (type);
