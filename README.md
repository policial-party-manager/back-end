party-cloud-backend
├── .mvn/wrapper                # Maven包装器，无需本地安装Maven即可构建项目
│   └── maven-wrapper.properties
├── src
│   ├── main
│   │   ├── java/com/party/party_cloud_backend
│   │   │   ├── PartyCloudBackendApplication.java  # SpringBoot项目启动主类
│   │   │   ├── controller      # 接口控制层，接收前端请求、参数校验、调用业务层
│   │   │   ├── service         # 业务逻辑接口层，定义业务功能抽象方法
│   │   │   │   └── impl        # service接口实现类，编写核心业务处理逻辑
│   │   │   ├── mapper          # MyBatis/MyBatis-Plus数据库操作层，操作数据表
│   │   │   ├── entity          # 数据库实体类，和数据表字段一一对应
│   │   │   ├── dto             # 数据传输对象，接收前端提交的请求参数
│   │   │   ├── vo              # 返回视图对象，封装后端返回给前端的响应数据
│   │   │   ├── config          # 全局配置类，跨域、MyBatis、拦截器、线程池等配置
│   │   │   ├── common          # 公共通用组件，统一返回结果、常量、全局拦截器等
│   │   │   ├── exception       # 全局自定义异常、统一异常处理器
│   │   │   └── utils           # 通用工具类，日期、加密、文件、字符串、Token工具等
│   │   └── resources
│   │       ├── static          # 静态资源（图片、js、css，前后端分离项目一般空置）
│   │       ├── templates       # Thymeleaf页面模板，纯接口后端基本不用
│   │       └── application.properties  # 项目全局配置文件（端口、数据库、Redis等）
│   └── test/java/com/party/party_cloud_backend
│       └── PartyCloudBackendApplicationTest.java  # SpringBoot单元测试启动类
├── .gitattributes              # Git文本换行格式配置
├── .gitignore                  # Git忽略文件配置，过滤target、本地配置、缓存文件
├── HELP.md                     # SpringBoot官方初始化帮助文档
├── mvnw / mvnw.cmd             # 跨系统Maven执行脚本（Linux/Mac、Windows）
├── pom.xml                     # Maven核心配置，管理项目依赖、打包插件、版本信息
└── README.md                   # 项目介绍、部署、接口说明文档