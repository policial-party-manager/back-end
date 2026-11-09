# 需求文档 vs 后端实现 · 模块级差距矩阵

> 调研日期：2026-11-08 ｜ 方法：需求侧用 python-docx 读全文（`D:\QQ\党建云平台项目框架.docx`：1296 段落 / 995 非空段落 / 19 张表 / 10611 字，另解压 `word/document.xml` 核对图文与修订标记）；实现侧取仓内一手来源（`openapi.yaml`、`sql/*.sql`、`sql/er-diagram.md`、`src/main/java/**`、`README.md`、`GLOSSARY.md`、`docs/research/`、`.out-of-scope/`、`scripts/e2e-acceptance.sh`、`git log`）。

**基线事实**：`openapi.yaml` 78 path / 96 operation（`GET 43 / POST 25 / PUT 16 / DELETE 12`）；`sql/databases.sql` 21 张表；20 个控制器；19 个 Service 实现；21 个实体。最新提交 `956d4f8`（2026-10-30）。

**矩阵计数：FULLY 3 ／ PARTIALLY 19 ／ NOT 21（共 43 行）**。其中 #27「地理围栏」一行是**反向项**——文档从未提及地理围栏（举证见 #27 行），该行记录的是实现侧自设的待办，为使差距完整而列出；若剔除，则为 FULLY 3 / PARTIALLY 19 / NOT 20（42 行）。另注：文档全文提及「地理围栏」为 0 次，但 `README.md:335` 把它列为未实现项 —— 该行存在的意义正是标注这个错位。

---

## 0. 文档本身的可读性边界（先声明，避免把"文档没说"误判成"需求缺失"）

| 事实 | 证据 | 影响 |
| --- | --- | --- |
| 全文 **0 张图片 / 0 个绘图对象 / 0 个文本框** | `word/document.xml` 中 `<w:drawing>`=0、`<w:pict>`=0、`w:txbxContent`=0；`word/_rels/document.xml.rels` 仅 8 条关系（theme/settings/fontTable/styles/numbering/endnotes/footnotes/webSettings），**无 `word/media/` 目录**，`[Content_Types].xml` 只有 `rels` 与 `xml` 两种扩展名 | 任务假设"heading 后跟图示"不成立：没有任何图示被丢失，文档本身就是**提纲式文本**；"流程如下："后面确实是空的 |
| **163 个段落以冒号结尾**（悬空引导句），其中 **63 个**后面紧跟的是章节标题或另一个引导句 | 逐段统计 | 大量小节只有"标题 + 引导句"，**具体规则从未写出**（见 §5 三个待裁决语义） |
| 章节号跳号：**无 3.7** | 3.6.13 → 3.8 | 文档缺陷，非本矩阵漏项 |
| 无修订痕迹、无批注、无域代码 | `w:ins`=0、`w:del`=0、`w:commentReference`=0、`w:fldSimple`=0、`w:instrText`=0 | 无"已废弃需求"标记可依 |
| 190 个段落带 `numPr`（真列表） | 逐段统计 | 列表项可用，但多数列表本身也是被引导句引出的半句 |

**文档结构**：第一章 1.1–1.7；第二章 2.1–2.12；第三章 3.1–3.6.13 + 3.8；随后为无编号的「接口设计」「数据库 E-R / 八张表」「后续开发顺序」。

---

## 1. 模块级差距矩阵

状态口径：**FULLY** = 文档要求被已实现端点/表完整覆盖且无实质差异；**PARTIALLY** = 有端点或表，但缺字段/缺规则/缺端点；**NOT** = 仓内无任何代码或表。

| # | 模块 | 文档要求（§ 号 + 原文/紧贴转述） | 现状 | 精确差距 | 文档 vs 实现是否矛盾 |
| --- | --- | --- | --- | --- | --- |
| 1 | 首页/仪表盘 | §3.2.1「进入首页后自动加载：本月党员发展人数／本月新增积极分子／本月新增党员／本周活动数量／待审核思想汇报／待审批身份调整／最近发布公告」；§3.2.2 支委首页仅本支部；§3.2.3 成员首页偏个人培养；§七 `GET /api/statistics/dashboard` 返回 `{member,activity,positive,target}` | **NOT** 无 `StatisticsController`；19 个 Controller 中无任何 dashboard/统计端点 | 缺 7 个卡片指标 + 1 个聚合端点；无"按角色返回不同首页数据"的服务 | 否（纯缺失） |
| 2 | 左侧动态菜单 | §3.3「系统采用动态菜单。菜单由后台根据角色返回。」+ 三套菜单 | **NOT** `MenuVo` 存在但**全仓无引用**（仅 `api/dto/MenuVo.java:9-13` 定义）；无菜单端点 | 缺 `GET .../menu`；`MenuVo` 是死代码 | 否 |
| 3 | 成员列表查询 | §3.4.1 列表页；§二 `GET /api/member/list?page&size&college&grade&major&branch&identity` | **PARTIALLY** `GET /api/v4/admin/users/page`（`AdminController.java:52`），参数 `page/size/keyword/branchId/roleId/status`（`openapi.yaml:562-620`） | 缺 **college / grade / major / identity** 四个过滤维度；文档的 `identity=积极分子` 无法按身份筛选；`keyword` 是文档没有的替代方案 | 否（能力收窄） |
| 4 | Excel 导入成员 | §2.6 表：学号*、姓名*、性别*、学院*、年级*、专业*、班级*、手机号、**政治身份***、所属支部；§3.4.2 表：文件格式「必须 xlsx」、学号非空非重、姓名非空、**学院/专业/班级不存在→提示管理员创建**、党支部空允许、手机号空允许；§3.4.2「导入结束后生成报告…管理员可下载错误报告」 | **PARTIALLY** `POST /api/v4/admin/users/import`（`AdminController.java:104`）；列 = 学号、姓名、手机号、邮箱、所属支部、角色、性别、学院、年级、专业、班级、紧急联系人、备注（`AdminUserServiceImpl.java:53-54`）；返回 JSON `ImportResult{total,successCount,failCount,errors[{row,reason}]}`（`api/dto/ImportResult.java:18-43`） | ① **无"政治身份"列**，导入无法落 `identity_id`（`AdminUserServiceImpl.java:541-556` 只写 13 列，不碰 identity）；② **无学院/专业/班级主数据表**（21 表中无对应字典），"不存在→提示创建"无法执行，这三列只当自由文本存；③ 校验放宽为 `.xlsx/.xls`（`ExcelUtil.java:82`）而文档写"必须 xlsx"；④ **无错误报告下载端点**，只有 JSON 内联 errors；⑤ 文档"党支部为空允许"已满足，但顺序/列数、`角色`列是文档没有的 | **是**：文档 `政治身份` 必填列 vs 实现无此列 |
| 5 | Excel 导出成员 | §3.4.5「批量导出」；§3.4.6 表 R4「导出Excel ✅/✅（本支部）」 | **NOT** `openapi.yaml` 无任何 export/导出 path（全仓 grep `export` 仅命中两个模板下载） | 缺导出端点；`ExcelUtil` 只有 `createTemplate` 与 `readDataRows`，**无写出数据行的方法**（`utils/ExcelUtil.java:19-120`） | 否 |
| 6 | 成员详情页 | §3.4.3 标签页；表：手机号√、邮箱√、**头像√**、学院×、班级×、学号×、姓名×、党员身份×（需走身份调整流程） | **PARTIALLY** `GET /api/v4/admin/users/{id}`（`AdminController.java:63`）→ `UserVo` 25 字段（`api/dto/UserVo.java:13-74`，含 identityId/identityName/contactPerson） | ① **全仓无 avatar 字段**（`grep -rn avatar` 零命中于 java/sql/yaml）；② 文档禁止改的 **学院/班级/学号/姓名**，管理员经 `PUT /api/v4/admin/users/{id}` 全部可改（`AdminUserServiceImpl.java:341-357` `fillDetail`） | **是**：§3.4.3 表「学院×/班级×/学号×/姓名×」vs 实现允许管理员改；文档只对"成员本人"设限、对管理员未设限，实现亦未区分 |
| 7 | 调整党支部 | §3.4.4「弹出…点击确认：系统执行：」+「**所有调整记录永久保留**」 | **PARTIALLY** 仅靠 `PUT /api/v4/admin/users/{id}` 带 `branchId`（`AdminUserServiceImpl.java:165-167` 校验 + `:354-356` 落库） | ① 无专用端点、无 `reason`、无生效时间；② **"永久保留"未实现**：无支部变更历史表，只有 ES 操作日志且不含 before/after（见 #10） | **是**：文档「永久保留」vs 实现的日志无前后值 |
| 8 | 批量操作 | §3.4.5「支持：批量导入／批量导出／批量调整党支部／批量设置培养联系人／批量停用／批量恢复。所有批量操作均需二次确认。」 | **NOT** 无任何 batch 端点（grep `batch|Batch` 于 `api/` 零命中）；仅"批量导入"存在（那是导入，非批量操作） | 缺 5 项批量能力；"二次确认"属前端，仓内无法判定 | 否 |
| 9 | 成员管理权限（支委） | §3.4.6 表：查看本支部成员 ✅支委、导出Excel ✅（本支部）、修改基础信息 ✅（本支部）；「支委只能管理所属党支部成员」 | **NOT** `AdminController` 类级 `@PreAuthorize("hasRole('SUPER_ADMIN')")`（`AdminController.java:35`），支委访问 `/api/v4/admin/users/**` 一律 403（`scripts/e2e-acceptance.sh:159` 断言成员访问管理端点 403） | `branch_admin` 对成员域**零权限**；无"按支部过滤的成员查询/编辑"服务 | **是**：§3.4.6 三行支委权限 vs 实现 403 |
| 10 | 操作日志（成员变更留痕） | §3.4.7「所有涉及成员数据的管理操作必须记录日志」；字段：操作时间、操作人、操作类型、操作对象、**修改前数据**、**修改后数据**、操作IP（可选）、操作结果 | **PARTIALLY** ES 文档 `OperationLog`（`log/OperationLog.java:19-96`）：logType/module/action/method/path/operatorId/operatorName/role/ipAddress/userAgent/success/resultCode/errorType/message/durationMs/occurTime；查询 `GET /api/v4/admin/logs/{page,stats,cleanup}`（`AdminController.java:393,410,416`） | ① **无"修改前数据/修改后数据"**，字段集里不存在载荷快照；② `tb_operation` 表存在但**从未被写入**：`OperationMapper` 全仓无注入点（`repository/OperationMapper.java` 无引用者）；③ `module/action` 由 URI 猜（`log/OperationLogFilter.java:84-142`），非业务语义 | **是**：文档要求的前后值 vs 实现只有 URI 级动作名 |
| 11 | 党员发展阶段字典 | §3.5.2「系统统一采用以下发展阶段。**系统任何地方均引用统一阶段，不允许自定义**」（清单本身缺失，见 §5）；§1.2 列出 6 阶段 | **PARTIALLY** `tb_identity`（`sql/tb_identity.sql`：identity_name/level/role_id/description），6 条播种（`sql/alter_identity_flow.sql:42-51`，`sql/databases.sql` 末尾同源）；超管可增改删（`AdminIdentityController.java:34,40,47`） | ① **"不允许自定义"与 `POST/PUT/DELETE /api/v4/admin/identities` 直接冲突**；② `level` 可空、无 1–6 约束（`tb_identity.sql:6`），无 CHECK；③ 无"字典不可删"保护 | **是**：§3.5.2「不允许自定义」vs 实现开放字典 CRUD |
| 12 | 当前身份展示 | §3.5.4「页面顶部始终显示当前党员发展阶段」 | **FULLY** `tb_user_detail.identity_id`（`sql/tb_user_detail.sql:20`）+ `UserProfileVo.identityId/identityName`（`api/dto/UserProfileVo.java:64-68`）+ `UserVo.identityId/identityName`（`UserVo.java:64-67`） | 无 | 否 |
| 13 | 身份调整（单人） | §3.5.5「系统不允许成员修改。仅：超级管理员／支委（所属党支部）拥有权限…整个过程无需学生参与」；§3.4.6 表 R7「调整党员身份：✅ / **提交申请** / ❌」；§2.7「学生不能自行修改身份；身份修改必须经过管理员或支委操作；每次修改生成历史记录」 | **PARTIALLY** 两阶段单据：`POST /api/v4/identity-changes`（支管/超管发起，`IdentityChangeController.java:40`）→ `POST /{id}/review`（**仅超管审批**，`:49`）；生效写回 `identity_id`（`IdentityChangeServiceImpl.java:112-121`）；状态机 1/2/3（`sql/tb_identity_change.sql`）；同一用户只允许一张待审批单（`:73-78`） | ① 文档从未要求"超管审批支委发起的变更"，实现增加了第二道门；② 超管自己发起时无需审批（`IdentityChangeServiceImpl.java:50-91` 直接落 pending，`approve` 才生效），文档写超管可"直接调整"；③ 无"驳回后重新发起=新单"之外的自动重试语义 | **是（文档内部先自相矛盾）**：§3.5.5 说支委"拥有权限"直接调整，§3.4.6 表又说支委只能"提交申请" |
| 14 | 批量身份调整 | §3.5.6「这是老师最常用的功能。例如：确定新一批积极分子。管理员进入：弹出：点击确认：系统：**无需逐人修改**。」 | **NOT** 无批量端点（`openapi.yaml` 无 batch path）；`tb_identity_change.user_id` 单值 NOT NULL，一单一用户 | 缺"选 N 人 → 一次确认 → N 条记录"的接口与事务 | 否（且文档未说明单/多单据，见 §5.1） |
| 15 | 身份历史 | §3.5.7「身份变化全部保留…点击某一条：查看详细信息…**系统禁止删除历史记录**」 | **PARTIALLY** `tb_identity_change` 全量保留，无删除端点（`IdentityChangeController` 只有 POST/GET）；`GET /managed/page`、`GET /{id}`、`GET /api/v4/user/identity-changes/mine/page` | ① **无"按某个成员查历史"的端点**（只有"我可管理的单据"与"我的单据"），管理员看某人轨迹需靠 `managed/page` 过滤；② `pageForAdmin` 支持 `userId` 过滤但未挂在任何 Controller 上（`IdentityChangeServiceImpl.java:182`，`AdminController` 无 identity-changes 入口） | 否 |
| 16 | 培养联系人 | §3.5.8「每位成员可设置：**1～2名培养联系人**…管理员点击修改。选择：…以后：思想汇报。培养记录。**均可关联培养联系人**」；§2.2.3 表「培养联系人 → 维护人员：**支委**」；§2.4「调整培养联系人」；§3.4.5「批量设置培养联系人」 | **NOT** 全仓 `grep 培养` **零命中**（java/sql/yaml/md 全部）；无 mentor/contact 关联表；`tb_user_detail.contact_person` 是**紧急联系人**（`sql/tb_user_detail.sql:16`，注释原文"紧急联系人"） | 缺 1:N 或 1:2 的培养联系人关系表与端点；缺"思想汇报关联培养联系人"；缺批量设置 | **是（同名不同概念，见 §4.1）** |
| 17 | 培养材料 | §3.5.9「系统支持上传培养过程中的材料…支持：PDF／Word／图片／**ZIP**」；§3.5.12 表 R7/R8 上传/删除培养材料 | **NOT** `POST /api/v4/uploads` 存在，但白名单 `pdf/doc/docx/jpg/png`（`LocalFileStorageServiceImpl.java:34`），`category` 白名单仅 `thought`（`:35`） | ① **ZIP 不支持**；② 无"培养材料"实体/表/端点，"材料"无处归档（只能挂思想汇报）；③ 无按成员维度列出材料的接口 | **是**：文档 PDF/Word/图片/ZIP 四类 vs 实现五扩展名但无 ZIP |
| 18 | 电子培养档案导出 PDF | §3.5.10「系统自动生成。无需人工维护…点击：导出。系统生成：**PDF**。方便打印」；§3.5.12 表 R9「导出培养档案 ✅✅ / 仅导出个人」 | **NOT** `pom.xml` 无任何 PDF 依赖（只有 POI，`pom.xml:72-75`）；无导出端点 | 缺档案聚合服务 + PDF 渲染 + 下载端点 | 否 |
| 19 | 培养统计 | §3.5.11「管理员可统计…支持：按 学院／专业／班级／党支部 统计」；§2.11「统计对象：人员统计／活动统计／培养统计」 | **NOT** 无任何统计端点；唯一汇总类是"我的学习汇总"（`GET /api/v4/user/study-records/mine/summary`，`UserStudyRecordController.java:38`） | 缺 4 个维度的分组统计；无 `GROUP BY college/major/class_name/branch_id` 的查询 | 否 |
| 20 | 思想汇报 | §3.5.1 列为模块项「思想汇报管理」；§1.2 要求统计「思想汇报完成情况」；§3.5.8「思想汇报…可关联培养联系人」 | **PARTIALLY** 提交/我的/撤回（`ThoughtReportController.java:36,43,52`）+ 审阅侧待审/详情/通过驳回（`ThoughtReportReviewController.java:37,46,53`）；审阅人=提交人支部书记自动派定（`ThoughtReportServiceImpl.java:51-64`，`GLOSSARY.md:9`）；状态机 1/2/3（`sql/tb_thought_report.sql`） | ① **无"完成情况"统计**（无按周期/应交人数的完成率）；② 无培养联系人外键（见 #16）；③ 文档未给出汇报频率/周期规则，实现亦无 | 否（文档对该模块几乎无规则描述） |
| 21 | 党校学习 | §3.5.1「党校学习记录」；§1.2「党校学习完成率」；§3.8 表 R7「查看党校课程 → 党校学习」；§1.7 四层架构 | **PARTIALLY** `tb_study`（name/type 自由文本，`sql/tb_study.sql`）+ `tb_study_record`（study_time `time`、score `decimal(5,2)`、一人一课一行，`sql/tb_study_record.sql:7-8`）；5 个端点（`AdminStudyCourseController.java:33,39,45`、`AdminStudyRecordController.java:39,46,54`、`UserStudyRecordController.java:30,38`） | ① **"党校"语义不存在**：无党校分类、无必修/选修枚举（`type` 自由文本，`er-diagram.md` 明示"不预设枚举"）；② **无"完成率"**；③ 课程无"党校"种子数据（`sql/*.sql` 无 `insert into tb_study`） | **是（术语错位，见 §4.4）** |
| 22 | 活动 CRUD | §3.6.5 表：活动名称*、活动类型*、所属党支部*、**活动负责人***（默认当前管理员）、活动地点*、活动简介、活动封面；表：开始*、结束*、签到开始（可提前30分钟）、签到截止（可延后30分钟）；「后台自动判断活动状态」 | **PARTIALLY** `POST/PUT/DELETE /api/v4/admin/activities`（`AdminController.java:238,245,253`）+ `PUT /{id}/status`（`:260`）；`ActivitySaveRequest` 13 字段（`api/dto/ActivitySaveRequest.java:14-43`）；`creator_id` = 当前操作者（`AdminActivityServiceImpl.java:94,116`）；`location` 为 `polygon` WKT + `location_description`（`sql/tb_activity_detail.sql:8-9`） | ① **"后台自动判断活动状态"未实现**：无 `@Scheduled`/`@EnableScheduling`（grep 零命中），状态只能人工 `PUT /status`（0/1/2，语义仓内未定义，`README.md:291` 仅说"取值域不同"）；② 地点类型是 `polygon` 而签到落 `point`（`sql/tb_sign_record.sql:9`），两者不互校；③ 无"签到窗默认±30分钟"的默认值逻辑 | **是**：§3.6.5「后台自动判断活动状态」vs 实现纯手工 |
| 23 | 活动类型字典 | §3.6.2 表 6 类（二课活动／组织生活／主题党日／党课学习／培训活动／其他活动）+ 是否签到／是否计入培养档案；「以后…只需要后台新增字典数据，无需修改代码」 | **PARTIALLY** `tb_activity_type`（name/need_sign/count_to_file，`sql/tb_activity_type.sql:5-7`）+ CRUD（`AdminController.java:267,273,280,287`）+ 只读 `GET /api/v2/content/activity-types`（`ContentController.java:75`） | ① **无种子数据**：6 类一个都没有（`sql/*.sql` 无 insert）；② `count_to_file` **有列无逻辑**：仅被读取用于回显/保存（`AdminActivityServiceImpl.java:180-181`），全仓无任何"计入档案"的判定；③ `name varchar(10)` 容不下"积极分子培训"这类值 | 否（能力缺口 + 死字段） |
| 24 | 活动列表/详情 | §3.6.4 列表页（"后台管理系统常见的数据表格布局"）；§3.6.6 详情页 | **FULLY** 管理侧 `GET /api/v4/admin/activities/page`（keyword/branchId/type/status）+ `/{id}`；内容侧 `GET /api/v2/content/activities/page` + `/{id}`（仅 status=1，`ContentController.java:58,68`）；`ActivityVo` 19 字段 | 文档两节均为悬空引导句，无字段级要求可比对 | 否 |
| 25 | 参与范围（目标人群） | §3.6.5「参与范围：管理员可自由选择。例如：也支持：适用于小规模培训。」；§2.9 签到校验「是否属于活动参与范围」 | **PARTIALLY** 表 `tb_activity_identity`（activity_id/target_identity/grade/college，`sql/tb_activity_identity.sql:5-8`）；**但无任何写入口**：`ActivitySaveRequest`/`ActivityVo` 均无 target 字段（各 0 命中），`ActivityIdentityMapper` 只被 `ActivitySignupServiceImpl.java:33` 读取 | ① 管理员**无法通过 API 设置参与范围**，唯一写入途径是 e2e 脚本里的裸 SQL（`scripts/e2e-acceptance.sh:128`）；② `grade int NOT NULL` 使"不限年级"无法表达；③ 签到不校验参与范围（见 #28） | **是**：§3.6.5「管理员可自由选择」vs 实现无写入口 |
| 26 | 二维码签到 / 60 秒刷新 | §3.6.7「这是整个模块核心。管理员点击：系统自动生成：二维码包含：二维码默认：**60秒自动刷新一次（可配置）**。避免截图签到」；§二 `GET /api/activity/qrcode/{id}`「返回二维码图片」 | **NOT** 无 qrcode/token/二维码端点（grep `qrcode|二维码|geofence` 于 java/yaml/sql 仅 1 条注释命中）；`README.md:335` 明列未实现 | 缺：二维码内容、签名 token、60s 轮换、过期校验、图片渲染 | 否（文档"二维码包含/默认"两句是悬空的，内容本身无法比对） |
| 27 | 地理围栏 | **文档全文未出现**"围栏/定位/经纬/距离/GPS/半径"（grep 命中 3 处均为"项目定位""定位就是"，与地理无关） | **NOT** `WktPointTypeHandler.java:22` 注释提到"地理围栏…再升级到 4326"，`README.md:335` 列 `[ ] 二维码 / token 签到、地理围栏` | 围栏是**仓内自设 TODO，不是文档需求**；签到 `location` 只是自由 WKT 字符串，无服务端距离校验（`SignRecordServiceImpl.java:76` 直接落库） | **是（反向）**：实现侧待办 vs 文档从未要求 |
| 28 | 成员签到 | §2.9「系统检查：用户是否登录；**是否属于活动参与范围**；是否重复签到；是否处于签到时间」；§3.6.13 表 7 条异常文案 | **PARTIALLY** `POST /api/v4/activities/{activityId}/sign`（`ActivitySignController.java:34`）；校验：活动存在→已发布(status=1)→签到窗→`need_sign=0` 拒绝→重复签到（`SignRecordServiceImpl.java:36-83`）；唯一约束兜底 `unique(activity_id,user_id)` | ① **缺"是否属于活动参与范围"**：`sign()` 全程不注入 `ActivityIdentityMapper`（对比 `ActivitySignupServiceImpl.java:33`），非目标人群可签到；② 文案与文档不一致（文档"您已完成签到"/"您无权参与本次活动"，实现"您已签到，请勿重复签到"/无越权文案）；③ **无"活动已结束禁止签到"** 的独立判断（只靠 sign_end） | **是**：§2.9 第 2 项校验 vs 实现缺失 |
| 29 | 报名 | §3.6.1「报名（可选）」；§3.6.5「参与范围」；§2.4 支委「查看报名情况」 | **FULLY** `POST/DELETE /api/v4/activities/{activityId}/signup`（`ActivitySignupController.java:32,38`）+ `GET /api/v4/admin/activities/{activityId}/signups/page`；报名即生效、开始前可取消、名额上限、年级/学院/身份三维校验（`ActivitySignupServiceImpl.java:124-153`） | 文档对该机制无细则，实现超出文档；**但仓内描述自相矛盾**：`ActivitySignupController.java:31` 的 `@Operation` 仍写"身份维度本期不校验"，而代码 `:141-142` 已校验、`openapi.yaml:4234` 也已写"政治身份" | **是（仓内自相矛盾，非文档矛盾）** |
| 30 | 请假管理 | §3.6.8「如果成员无法参加。管理员可提前录入：或者：成员在微信小程序提交请假申请（**可作为二期功能**）」，表 4 状态：正常签到／**请假**／**缺席**／**迟到** | **NOT** 无请假实体/表/端点；`tb_sign_record.sign_type` 取值域仅 `sign`（本人签到）/`makeup`（补签）（`sql/tb_sign_record.sql:8`） | 缺 3 个状态（请假/缺席/迟到）；缺管理员代录请假；`sign_type` 与文档的"请假状态"是两套正交概念 | **是**：文档 4 状态 vs 实现 2 种签到渠道 |
| 31 | 活动统计 | §3.6.9「活动结束：系统自动生成：例如：支持：包括：**签到名单。缺席名单。请假名单。**」；§二 `GET /api/activity/statistics`；§3.6.12 表 R9「导出签到数据 ✅✅」 | **NOT** 无 per-activity 统计端点，只有原始明细分页 `GET /api/v4/admin/activities/{id}/signs/page`（`AdminSignRecordController.java:31`）与报名分页 | 缺参与率、缺三类名单的派生视图、缺导出；`缺席/请假` 名单在数据模型上根本无法生成（无请假表、无应到名单） | 否（纯缺失，但分母语义见 §5.2） |
| 32 | 活动归档 | §3.6.10「活动结束：自动进入：管理员：**不能删除签到记录**。只能：…复制。修改时间。重新发布」 | **NOT** 无归档态；`DELETE /api/v4/admin/activities/{id}` **物理删除**主表与详情（`AdminActivityServiceImpl.java:126-132`），`tb_activity_detail` FK `on delete cascade`（`sql/tb_activity_detail.sql:14`） | ① 无归档标记；② **活动可被物理删除**，而 `tb_sign_record`/`tb_activity_signup` 对 `tb_activity` 的 FK **无 on delete**（`sql/tb_sign_record.sql:11-13`），有签到记录时删除会触发外键错误（500）而非"禁止删除"；③ 无"复制活动"端点 | **是**：§3.6.10「不能删除签到记录」vs 实现允许删活动（并撞 FK） |
| 33 | 成员活动记录 | §3.6.11「每位成员自动生成：例如：这些记录自动同步到：无需再次录入」 | **NOT** 无"我的活动"端点（`/api/v4/user/**` 只有 profile、study-records、identity-changes） | 缺按成员聚合的签到+报名时间线 | 否 |
| 34 | 活动权限（支委） | §3.6.12 表：创建活动 ✅（本支部）、编辑 ✅（本支部）、查看本支部活动 ✅、生成二维码 ✅、查看签到情况 ✅、导出签到数据 ✅ | **NOT** 活动域全部端点类级 `hasRole('SUPER_ADMIN')`（`AdminController.java:35`、`AdminSignRecordController.java:24`、`AdminActivitySignupController.java:23`）；支委 403 | `branch_admin` 对活动域**零权限**（不能建、不能看签到）；无"本支部"过滤分支 | **是**：§3.6.12 六行支委权限 vs 实现 403 |
| 35 | 数据统计 | §2.11 统计对象：人员统计／活动统计／培养统计；§七 `GET /api/statistics/{dashboard,activity,member}`；§1.2 七个自动统计项（含"二课学时统计""党校学习完成率""思想汇报完成情况""活动参与率"） | **NOT** 无统计模块；唯一 `stats` 端点是日志统计 `GET /api/v4/admin/logs/stats`（`AdminController.java:410`） | 缺 3 个统计端点、7 个指标；无任何跨表聚合查询（`GROUP BY`）在业务域出现 | 否 |
| 36 | 新闻公告 | §3.2.1「最近发布公告」；§五 `GET /api/news/list`、`/api/notice/list`；表结构 §tb_news/§tb_notice；`tb_notice.top_flag` | **PARTIALLY** 新闻：后台 CRUD + 上下线（`AdminController.java:298-335`）+ 只读分页/详情（`ContentController.java:30,39`）；公告：CRUD + 发布 + 展示期（`start_time/end_time`）+ 3 态（`AdminController.java:346-382`，`sql/tb_notice.sql`） | ① **公告置顶未实现**：`tb_notice_top` 表 + `NoticeTop` 实体 + `NoticeTopMapper` 全部存在但**零引用**（`noticeMapper` 之外无使用），文档 §tb_notice 的 `top_flag` 落在此处；② 文档 `tb_news.publish_time` vs 实现 `create_time/update_time`（无 publish_time 列）；③ `tb_news.title varchar(10)`（`sql/tb_news.sql:6`）对比文档"党建新闻标题"明显过窄；④ 文档 `tb_notice.publisher` 字符串 vs 实现 `publisher_id int`（`sql/tb_notice.sql:10`） | **是（字段名/类型级）**：`top_flag` 缺、`publish_time` 缺、`publisher` 类型不同 |
| 37 | 下载专区/资源 | §五 `GET /api/resource/list`、`GET /api/resource/download/{id}`；§tb_resource 表（title/category/file_name/file_url/**download_count**/upload_user）；category：申请书模板／思想汇报／学习资料／常用表格／其他；§3.8 表 R5/R6「下载模板→首页→下载专区」 | **NOT** `tb_resource` 未建表：`sql/er-diagram.md:5` 明写「`tb_resource` **未采用**」；4 个 category 无落点 | 缺表、缺端点、缺下载计数；"思想汇报模板""入党申请书模板"作为资源实体不存在（现仅有 Excel 导入模板下载） | **是**：文档把 tb_resource 列为第八张表 vs 实现明确弃用 |
| 38 | 党建成果 / 党建网站 | §五 `GET /api/achievement/list`、`GET /api/website/list` | **NOT** 21 表中无 achievement/website/link 类表；无端点；文档"数据库 E-R 图"一节也未画这两张表 | 文档自身在接口章列了端点、在表章未给表 —— 无实现落点 | 否 |
| 39 | 智能助手 | §3.8「右下角放一个悬浮按钮…功能导航（最重要）…其实背后可以非常简单。**完全不用接数据库**。就准备几十条问答…甚至可以做成关键词匹配」；表 7 条问答；§八 `POST /api/ai/chat {"message"} → {"answer"}` | **NOT** 无 ai/chat 端点；grep `chat|assistant|智能|llm|openai` 于 java/yaml 仅命中测试类里的 `MapperBuilderAssistant` | 缺端点、缺问答字典。注意：文档明确"不用接数据库、几十条问答、关键词匹配"，实现侧连静态字典都没有 | 否 |
| 40 | 登录 / SSO | §2.2.1「系统采用学校统一身份认证平台（SSO）…**平台不保存用户密码，不提供独立注册和密码修改功能**」；账号=学号/工号（§2.2.1）；§2.2.3 表 3 项字段"不可修改"；§一 `GET /api/auth/login` → 跳转统一认证，`/callback`，`/current`，`POST /logout` | **PARTIALLY** CAS 已接：`GET /api/v1/auth/sso/login` + `/sso/callback`（`AuthController.java:121,131`）；**另有 4 条本地登录通道**：`/login/userpass`（BCrypt 校验 `tb_user.password`，`AuthServiceImpl.java:52-66`）、`/login/email`、`/login/phone`、`/verifyCode`（`AuthController.java:45,60,75,90`）；JWT access+refresh（`/refresh`、`/logout` 吊销） | ① **密码入库**：`tb_user.password varchar(64) default md5('123456')`（`sql/tb_user.sql`），种子超管 `superadmin/123456`（`InitSuperAdminRunner.java:33-40`）；② 文档的"不提供注册"已满足（无注册端点，只能管理员建号/导入），"不提供改密"也已满足（只有 `PUT /admin/users/{id}/password` 管理员重置，`README.md:340`）；③ **缺 `GET /api/auth/current`**：无 current 端点，替代为 `GET /api/v4/user/profile`（`UserController.java:31`），`api/dto/CurrentUserResponse.java` 是**死代码**（零引用）；④ CAS 仅允许已存在账号（`SsoServiceImpl.java:110-113`"账号未开通，请联系管理员"），文档未提这条约束 | **是**：§2.2.1「不保存用户密码」vs 实现密码+邮箱码+手机码+CAS 四通道并存；§2.2.1 隐含"SSO 是唯一入口" vs 实现 SSO 是可选补充 |
| 41 | 角色权限 RBAC | §1.6「RBAC…将系统角色与党员身份分离管理」；三角色：超级管理员／**支部管理员（支委）**／普通成员；§2.3 三套权限清单；§3.4.6/§3.5.12/§3.6.12 三张权限矩阵；§2.3.1「设置支委」；§tb_user 字段 `role: teacher/branch_admin/student` | **PARTIALLY** `tb_user`+`tb_user_role`+`tb_role`+`tb_role_permission`（四表 N:M）；`role_name` CHECK 必须以 `ROLE_` 开头（`sql/tb_role.sql`）；JWT 装 `ROLE_<大写>`（`filter/JwtAuthFilter.java:33-39`）；方法级 `@PreAuthorize` | ① **权限名不参与鉴权**：`tb_role_permission` 的 7 个权限名（`AdminRoleServiceImpl.java:29-37`）在授权链路上**零引用**，代码注释自认"用于维护与后续细粒度鉴权预留"（`:23`）；② 文档的 `teacher` 角色码**不存在**（grep `teacher` 零命中），等价物是 `super_admin`；③ 文档"支委"= `branch_admin`，但实现的 `branch_admin` 管辖范围定义为"**本人担任秘书的支部**"（`IdentityChangeServiceImpl.java:200-205`、`StudyRecordServiceImpl.java:216-218`），而非"所属支部"；④ 三张权限矩阵中支委的成员/活动/培养权限全部 403（见 #9/#34/#19/#33） | **是**：三张权限矩阵 vs 实现（详见 §4.3） |
| 42 | 组织关系调整 | §2.10「用于：转专业／支部调整／支委换届。流程：」；§3.4.4 调整党支部「所有调整记录永久保留」 | **PARTIALLY** 支部调整：管理员改 `branchId`（同 #7）；**转专业无落点**（`major` 只是自由文本，无变更记录）；无独立"组织关系调整"单据 | 缺统一单据（类型/原因/生效日/审批）、缺转专业的组织关系语义、缺历史 | 否 |
| 43 | 支委换届 | §2.10「支委换届」列为组织关系调整场景之一 | **PARTIALLY** 仅能改 `tb_branch.secretary_id`（`AdminBranchServiceImpl.java:86-88`）；无换届单据、无任期、无历史 | 换届=直接改指针。**副作用**：`secretary_id` 同时是身份变更/学习记录/思想汇报派审的权限锚点（§41-③），改一次即静默改变三处数据可见范围 | 否（文档只列了场景名） |

---

## 2. 术语与概念陷阱（逐条裁定）

### 2.1 文档「培养联系人」 vs 表 `tb_user_detail.contact_person`

**结论：两个完全不同的概念，且实现里不存在文档说的那个。**

| 维度 | 文档「培养联系人」（§3.5.8） | 实现 `contact_person` |
| --- | --- | --- |
| 中文名（仓内原文） | 培养联系人 | **紧急联系人**（`sql/tb_user_detail.sql:16`；`openapi.yaml:485` 等 7 处 description 均为"紧急联系人"） |
| 基数 | **1～2 名**（§3.5.8「每位成员可设置：1～2名培养联系人」） | 单值 `varchar(50)`，一个人名 |
| 谁维护 | **支委**（§2.2.3 表「培养联系人 → 维护人员：支委」；§3.5.8「管理员点击修改」） | **成员本人**可改：`UserProfileUpdateRequest.contactPerson`（`api/dto/UserProfileUpdateRequest.java:61-63`）→ `UserServiceImpl.java:117-120` 直接落库；管理员亦可改（`AdminUserServiceImpl.java:352`） |
| 用途 | 「以后：思想汇报。培养记录。**均可关联培养联系人**」——是培养业务的**关联主体** | 纯联系信息，无任何业务关联；全仓无外键、无查询引用 |
| 批量能力 | §3.4.5「批量设置培养联系人」 | 无（见 #8） |

即：文档要的是"党员培养责任人"（可 2 人、由支委指派、思想汇报与培养记录挂靠），实现给的是"紧急联络电话"（1 个字符串、本人自填）。**二者不可互换，也不是同一字段的中英对译**。仓内 `培养` 二字零命中，说明该概念从未进入实现词汇表（`GLOSSARY.md` 15 条术语中亦无）。

### 2.2 文档「党员身份 / 党员发展阶段 / 培养阶段」 vs 仓内「当前政治身份」

- **仓内权威定义**：`GLOSSARY.md:7`「**当前政治身份**」= 用户此刻的身份（六级之一），存 `tb_user_detail.identity_id`，NULL=未登记；恒等于最新一条已生效变更单的 `to_identity`。
- **文档用法**：三种叫法混用 —— 「党员身份」（§1.6「党员身份表示学生所处的发展阶段」）、「党员发展阶段」（§3.5.2 标题）、「培养阶段」（§1.2「不同培养阶段的信息管理」）。文档 §1.6 明确「系统角色 ≠ 党员身份」，与仓内 `GLOSSARY.md:14`「`role_id` 仅展示映射，**永不参与鉴权**」同向。
- **文档自身的阶段清单三处不一致**（这是真陷阱）：
  1. §1.2：「普通学生、入党申请人、积极分子、发展对象、预备党员、正式党员」= **6 项**（与实现 `tb_identity` 播种 1–6 完全一致，`sql/alter_identity_flow.sql:42-51`）；
  2. §2.5：「普通学生／入党申请人／积极分子／发展对象／预备党员／正式党员」= **6 项**（同上）；
  3. §tb_member 字段说明：`identity` 取值「普通同学／积极分子／发展对象／正式党员」= **4 项**，**缺"入党申请人"与"预备党员"**，且"普通学生"写作"普通同学"。
  4. §3.5.2 本该是权威清单（「系统统一采用以下发展阶段」），但**清单本身缺失**（该标题后直接是 §3.5.3，见 §5.3）。
- **实现取的是 6 项版本**，且用 `level 1..6` 排序（`README.md` 政治身份级别表）。→ **文档内部矛盾，实现选了其中一支**；`level` 列可空且无 CHECK（`sql/tb_identity.sql:6`），文档的"阶段"顺序性在库层无约束。

### 2.3 「支委」 vs `branch_admin`；「超级管理员（老师）」 vs `super_admin`

| 文档 | 实现 | 是否等价 |
| --- | --- | --- |
| 支部管理员（支委）（§1.3 表、§1.6 表、§2.4） | `branch_admin`（`model/enums/UserRole.java:11`）；`tb_role.role_name` 存 `ROLE_BRANCH_ADMIN` 形态（CHECK 前缀 `ROLE_`） | **名称等价，语义不等价**：文档的支委能管本支部成员/活动/培养记录；实现的 `branch_admin` 只能：思想汇报审阅（`ThoughtReportReviewController.java:30`）、身份变更发起（`IdentityChangeController.java:40`）、本支部学习记录（`AdminStudyRecordController.java:31`）。成员域与活动域一律 403（#9/#34） |
| 超级管理员（老师）（§1.3 表 R1、§2.3.1、§3.4.6 表列头"老师"） | `super_admin` | **等价**（`README.md` 系统角色表；`AdminController.java:35`）。但**文档提出的角色码是 `teacher`**（§tb_user 字段 `role: teacher/branch_admin/student`；§一 返回体 `"role":"teacher"`）—— `teacher` 在仓内**零命中**，且 `tb_user` **没有 role 列**（角色在 `tb_user_role`，`sql/tb_user.sql` 只有 id/username/password/三个时间/status） |
| 「设置支委」（§2.3.1 权限项） | 无专门动作；支委身份 = 给用户分配 `branch_admin` 角色 + 在 `tb_branch.secretary_id` 指向他（两处独立维护，无一致性校验） | **不等价**：文档把"设置支委"当一次操作，实现需要两步且可只做一半 |

补充：`InitSuperAdminRunner` 把 `roleId` 硬编码为 `1L`（`service/runner/InitSuperAdminRunner.java:39`），假设 `tb_role.id=1` 是超管角色 —— 文档无此约定。

### 2.4 「党校学习」 vs `tb_study` / `tb_study_record`

- 文档「党校学习」出现在 4 处：§1.2「党校学习记录」「党校学习完成率」、§3.1.1「思想汇报、党校学习等党建业务」、§3.5.1「党校学习记录」、§3.8 表「查看党校课程 → 党校学习」。文档给出的是**一个并列业务域**，与"活动""思想汇报"同级。
- 实现 `tb_study` 是**通用课程字典**：`name`、`type`（`varchar(20)` 自由文本，`sql/tb_study.sql`），`er-diagram.md` 明写「`type` 自由文本（varchar(20)），**不预设枚举**」。
- 裁定：**结构上可承载，语义上未承载**。仓内 `党校` 二字**零命中**（java/md/sql/yaml 全无）；无党校专属分类、无必修/选修枚举、无"完成率"、无种子课程。文档的"党校学习完成率"在实现里无任何对应聚合（`StudySummaryVo` 只有 `recordCount/totalMinutes/avgScore`，`api/dto/StudySummaryVo.java`）。

### 2.5 「二课学时 / 二课活动」 vs 仓内

- 文档「二课活动」是**活动类型之一**（§3.6.2 表 R1「二课活动｜志愿服务、红色实践｜✅签到｜✅计入培养档案」；§2.8 活动类型清单首位；§2.5「二课活动等签到」）。
- 文档「二课学时统计」是 §1.2 的 7 个自动统计项之一。
- 实现侧：
  - **「二课活动」可表达**——作为 `tb_activity_type` 的一行 `name`。但 `tb_activity_type` **无种子数据**，需管理员手工建；且 `name varchar(10)` 够用。
  - **「二课学时」无落点**——`tb_activity` / `tb_activity_detail` / `tb_sign_record` / `tb_activity_signup` **均无学时字段**；唯一叫"学时"的是 `tb_study_record.study_time time`（`sql/tb_study_record.sql:7`），那是**课程学时**，与二课活动无关。
  - `tb_activity_type.count_to_file`（"是否计入档案"）是文档 §3.6.2 第 4 列的落点，但**有列无逻辑**：全仓只被读写、从不参与判定（`AdminActivityServiceImpl.java:180-181`）。
- 裁定：**「二课学时」在仓内不存在，且无任何表可折算**（活动无时长学分、签到无学时）。

### 2.6 文档提议的 API 路径 vs 冻结的 v1/v2/v4

文档「接口设计」章（§一–§八）给出 11 个一级资源前缀，**与实现的版本前缀体系完全不兼容**：

| 文档路径 | 实现路径 | 差异性质 |
| --- | --- | --- |
| `GET /api/auth/login`（跳转统一认证）、`/api/auth/callback`、`/api/auth/current`、`POST /api/auth/logout` | `/api/v1/auth/sso/login`、`/sso/callback`、`（无 current）`、`/api/v1/auth/logout` | 前缀 + 动词路径缺 `/v1`；`current` 缺 |
| `GET /api/member/list?page&size&college&grade&major&branch&identity`、`/api/member/{id}`、`POST /api/member`、`PUT /api/member/{id}`、`DELETE /api/member/{id}`、`POST /api/member/import`、`GET /api/member/export` | `/api/v4/admin/users/{page,{id},import,template}` | **资源名不同**（member → users）+ 无 `DELETE /users/{id}`（用户只停用不删）+ 无 export |
| `PUT /api/member/changeIdentity` body `{memberId, identity}` | `POST /api/v4/identity-changes` body `{userId, toIdentity, reason}` + `POST /{id}/review` | **动词进路径** vs 资源化；且实现是两阶段单据，文档是单步改值 |
| `GET /api/activity/list`、`/{id}`、`POST /api/activity`、`PUT/DELETE /{id}`、`POST /api/activity/publish/{id}`、`GET /api/activity/qrcode/{id}`、`GET /api/activity/sign/{id}`、`GET /api/activity/statistics` | `/api/v4/admin/activities/{page,{id}}`、`PUT /{id}/status`、`/api/v2/content/activities/{page,{id}}`；**qrcode / sign/{id} / statistics 均无** | 无 `publish`（改为 status）、无 qrcode、无 statistics；`sign` 改为 `/api/v4/admin/activities/{activityId}/signs/page` |
| `POST /api/sign/checkin` body `{activityId, studentId}`、`GET /api/sign/my`、`GET /api/sign/list/{activityId}` | `POST /api/v4/activities/{activityId}/sign` body `{device,location,remark}`；**无 `/my`**；`GET /api/v4/admin/activities/{activityId}/signs/page` | ① **`sign` 前缀整块不存在**（无 SignController，签到并入 activities 资源）；② 文档用 `studentId` 定位用户，实现一律取 JWT 里的 `userId`（`@CurrentUser User`）——**不接受外部传入 identity，这是安全上更强的一侧**；③ `/my` 缺 |
| `GET /api/news/list`、`/api/notice/list`、`/api/achievement/list`、`/api/website/list`、`/api/resource/list`、`/api/resource/download/{id}` | `/api/v2/content/news/{page,{id}}`、`/notices/{page,{id}}`；**achievement / website / resource 全无** | 内容域前缀为 `/api/v2/content`；4 个资源缺 2 个半 |
| `GET /api/user/profile`、`PUT /api/user/profile`、`POST /api/user/avatar` | `GET/PUT /api/v4/user/profile`；**无 avatar** | 前缀 + avatar 缺（无 avatar 字段，见 #6） |
| `GET /api/statistics/dashboard`、`/activity`、`/member` | **全无** | 统计模块整体缺 |
| `POST /api/ai/chat` | **无** | 智能助手整体缺 |

**根因（仓内已成文）**：`README.md:230-236`「版本号**冻结**：不新增 `/api/v5`，新端点并入其所属域的现有前缀」；`docs/research/2026-10-api-versioning-contract.md:6`「版本号是『重组目录时顺手加的』，没有版本策略」。→ 文档的 `/api/*` 扁平前缀**不可能被采纳**，讨论时须以 `openapi.yaml` 的 78 path 为唯一基线。
附带：`GLOSSARY.md:15` 写「当前 81 path==81」与 `openapi.yaml` 实际 78 path、`README.md:176`「78 条 path / 96 个 operation」**三处数字不一致**（GLOSSARY 陈旧）。

### 2.7 「不保存密码、不提供注册和改密，用学校 SSO」 vs 实现的四通道登录

文档原文（§2.2.1，逐字）：「系统采用学校统一身份认证平台（SSO）进行用户身份验证。」「**平台不保存用户密码，不提供独立注册和密码修改功能。**」「用户账号来源：学生：学校学号；教师：学校工号。」

实现逐条对照：

| 文档断言 | 实现 | 判定 |
| --- | --- | --- |
| 不保存用户密码 | `tb_user.password varchar(64) NOT NULL default md5('123456')`（`sql/tb_user.sql`）；BCrypt 存储与校验（`AuthServiceImpl.java:61`）；`InitSuperAdminRunner.java:33-40` 播种 `superadmin/123456`；默认密码常量 `AdminUserServiceImpl.java:50` `DEFAULT_PASSWORD = "123456"` | **矛盾**（明文约定密码+默认弱口令） |
| 不提供注册 | 无注册端点（`AuthController` 8 个 path 全为登录/验证码/刷新/退出/SSO）；建号只能 `/api/v4/admin/users` 或 `/import` | **一致** |
| 不提供密码修改 | 无自助改密端点；仅 `PUT /api/v4/admin/users/{id}/password` 管理员重置（`AdminController.java:91`）；`README.md:340` 明列未实现「用户自助改密 / 忘记密码找回」 | **一致**（但实现是"没做"，文档是"不做"，动机不同） |
| SSO 是身份验证方式 | CAS 已接但**是补充通道**：另有 `/login/userpass`、`/login/email`、`/login/phone`、`/verifyCode`（`AuthController.java:45,60,75,90`）；且 CAS 仅放行**已存在账号**（`SsoServiceImpl.java:110-113`），即"SSO 不是开户口" | **矛盾**（SSO 非唯一，且依赖本地账号先存在） |
| 账号 = 学号/工号 | `tb_user.username varchar(10)` 存学号（`sql/tb_user.sql`）；**无"工号"概念**，教师与学生在同一 `tb_user`，仅角色不同；`tb_user` 无 `user_no` 列（文档 §tb_user 列了 `user_no`「统一身份号」与 `username` 两列，实现只有一列 `username`） | **部分矛盾**（文档的 `user_no` 列不存在） |

另：手机号验证码通道**结构性存在但不可用** —— `.out-of-scope/sms-verification.md` 记录了拒绝决定；`AuthServiceImpl.java:129-131` 验证码只写日志。

---

## 3. 三个需要当场裁决的语义（文档侧证据不足）

### 3.1 §3.5.6 批量身份调整：一张批次单还是 N 张单人单？

**文档没有任何一句话说明。** §3.5.6 全文（含引导句，逐段照抄）：

> 3.5.6 批量身份调整
> 这是老师最常用的功能。
> 例如：
> 确定新一批积极分子。
> 管理员进入：
> 弹出：
> 点击确认：
> 系统：
> 无需逐人修改。

第 5–8 句全部是悬空引导句（"管理员进入："后面没有宾语，"系统："后面没有动作），**"系统："之后为空**，因此"生成 1 张批次单 / N 张单人单 / 直接改值不留单"三种可能都无法从文档排除。全文另无"批次""单据""一次提交"等表述（grep 无命中）。

可用的**弱信号**（仅为旁证，不构成规定）：
- 「无需逐人修改」限定的是**交互方式**（一次操作），不等于**数据形态**（一张单）。
- §3.5.7「点击某一条：查看详细信息」暗示历史列表是**逐条**的（e 人 e 条更自然）。
- §3.5.5 单人流程同样悬空，无法反推批量应复用还是另立。

**实现侧的硬约束**（如要落地需先破）：`tb_identity_change.user_id int NOT NULL` 一单一人（`sql/tb_identity_change.sql:4`），且同一用户只允许一张待审批单（`IdentityChangeServiceImpl.java:73-78`）。即当前模型**只能表达 N 张单人单**，要实现"批次单"必须新增 `batch_id` 或新表。

### 3.2 §3.6.9 活动统计的参与率分母（报名/应到/实到/请假）

**文档从未给出公式或分母定义。** 全文涉及参与率的只有 3 处，且都是名词罗列：
- §1.2「（7）数据统计分析 自动统计：…活动参与率…」（列表项，无定义）
- §3.6.1「活动结束后，系统自动统计签到情况、参与率」（无定义）
- §3.6.9「活动结束：系统自动生成：例如：支持：包括：签到名单。缺席名单。请假名单。」（"系统自动生成："与"支持："后均悬空）

**关键：全文没有"应到""实到""报名人数""参与率=" 等任何字样**（grep `应到|实到|分母|率=` 零命中；"率"仅出现在"参与率""完成率"两个名词里）。因此 4 个候选分母**都无文档依据**：

| 候选分母 | 文档依据 | 实现可行性 |
| --- | --- | --- |
| 报名人数 | 无（§3.6.1 把报名写成"（可选）"） | 有 `tb_activity_signup`，但报名可选 → 未开报名时无分母 |
| 应到（目标人群展开） | 无 | 只能由 `tb_activity_identity`（身份/年级/学院）反查 `tb_user_detail`，且**该表无写入口**（#25） |
| 实到 | 无 | 有 `tb_sign_record` |
| 请假 | 无 | **无请假表/字段**（#30），此分母当前不可能 |

再叠加两层缺口：① 文档要求的「缺席名单」无法生成——缺席=应到−实到，而"应到"在当前数据模型下算不出（`tb_activity_identity.grade NOT NULL` 使"不限年级"不可表达）；② 「请假名单」完全无落点（`sign_type` 只有 `sign`/`makeup`）。→ **活动统计不是"没写代码"，而是"分母在库里不存在"**，必须先定分母再定表。

### 3.3 §3.4.5 批量操作的具体语义

§3.4.5 全文只有 8 行，是纯清单：

> 3.4.5 批量操作
> 支持：
> 批量导入
> 批量导出
> 批量调整党支部
> 批量设置培养联系人
> 批量停用
> 批量恢复
> 所有批量操作均需二次确认。

- **有明确要求的部分**：「所有批量操作均需二次确认」—— 属前端交互，仓内无法判定（见 §6）。
- **完全无定义的部分**：选择方式（勾选/按条件筛选/粘贴名单）、单次上限、部分失败时是"全成功或全失败"还是"逐条报错"、批量停用后是否可恢复（`status` 语义：`tb_user.status` 1 启用/2 停用，`sql/tb_user.sql`）、"批量恢复"与"批量停用"是否对称。
- **与实现已有的唯一交集**：「批量导入」= 逐行 try/catch、部分成功、返回 `ImportResult{total,successCount,failCount,errors[]}`（`AdminUserServiceImpl.java:228-241`）—— 这是一个**已定型的"部分成功"语义**，若批量调整身份沿用同一风格则与"二次确认"配合自然；但文档没说要沿用。
- 「批量设置培养联系人」「批量导出」「批量恢复」三项在实现里**连单体版本都没有**（#16/#5；`tb_user.status` 只有 2 值，无独立的"恢复"动作，只有再次 `PUT /status` 传 1）。

---

## 4. 近期工作方向（`git log --oneline -25`）

时间线两段式，**最后提交 2026-10-30，距今 9 天无新提交**：

**第一段 2026-09-05 ～ 2026-10-28（基建与契约收敛）**
`8bf2631` CAS SSO 接入 → `7b96eba` openapi 更新 → `c24a2a6`/`02ac766` README 两次重写 → `e777b15` 清洗 DDL + 一键建库 + schema-verify → `f34526b` 技能配置 → `fabc374` 删空壳 `HistoryController` + 控制器鉴权契约测试 → `896f9fc` 统一 `tb_branch.status` 语义 → `64a38d1` 导入模板 Swagger 描述补齐 + 模板契约测试 → `4d60f22` **双轨 API 调研**（产出 `docs/research/2026-10-api-versioning-contract.md`，定"openapi 唯一契约 + 版本冻结"）。

**第二段 2026-10-30（一天 11 个提交，功能密集落地，全部带 issue 号）**
`6aeda78` 活动签到（#35，POINT TypeHandler + DDL 放宽）→ `840284a` 活动报名（#53，报名即生效/名额/年级/学院）→ `149557d` 个人中心 DTO 化（#36，收紧 `branchId` 自助修改）→ `d9981d2` v2/branch 标废弃（#51 步1-2）→ `3198808` 思想汇报（#33，自动派审 + 本地附件 + 状态机）→ `d9e7e6e` 学习课程/记录（#32，upsert + 支部权限面 + 汇总）→ `e5b4ef7` **政治身份体系（#44，当前身份落库 + 字典播种 + 两阶段变更 + 角色解耦红线）** → `b64e305` 报名接通身份维度（#61）→ `c5c3b55` **E2E 全链路验收脚本 30 断言 + GLOSSARY 首批词汇**（E2E 抓出 #63）→ `462cd1f` `@PreAuthorize` 拒绝改 403（#63）→ `6001134` 删除 `/api/v2/branch`（#51 步3，双轨合拢）→ `956d4f8` SMS 通道拒绝决定（#37，产出 `.out-of-scope/sms-verification.md`）。

**方向判读（仅就已有痕迹）**：
1. **刚做完的主线是"党员发展 + 活动"两条业务链的竖切**：身份体系（含审批）、思想汇报、学习记录、报名、签到 —— 五件事都在 2026-10-30 落地，且 `scripts/e2e-acceptance.sh` 已把这五条串成一条 30 断言的可重跑链。
2. **配套的横切是"契约与语义收敛"**：openapi 单一契约、版本冻结、v2/branch 下线、`@PreAuthorize` 403、`branchId` 逃逸字段封堵、`status` 语义统一 —— 近期工作**明显偏向"把已有面做严"而非"扩大面"**。
3. **尚未被触碰的是文档里的四大块**：统计/仪表盘（#1/#19/#31/#35）、二维码与请假（#26/#30）、培养域（#16/#17/#18）、支委权限面（#9/#34）—— 这四块在 2026-10-30 的 11 个提交里**一次都没出现**。
4. **支委（branch_admin）权限面是"做了一半"的状态**：`#32`/`#33`/`#44` 三个提交都自带"支部权限面"（`managedBranchIds`），但只覆盖学习记录/思想汇报/身份变更；成员域与活动域仍是 `SUPER_ADMIN` 独占。`README.md:341` 自己把这个记为未实现项。

---

## 5. 无法从一手来源判定

| 事项 | 为什么判不了 |
| --- | --- |
| **前端行为**（页面布局、批量操作"二次确认"、动态菜单渲染、`§3.2` 首页卡片点击跳转、`§3.4.1` 列表页交互） | 工作区只有 `back-end/`，**无前端仓库**。`docs/research/2026-10-api-versioning-contract.md:18` 已记录同一结论：「**不存在**前端仓库（同级目录只有 `back-end/`），因此『前端实际调用表』无法用一手来源回答」。所有"需二次确认""弹出窗口"类要求均无实现侧落点可比对 |
| **微信小程序侧**（`§1.7`「微信小程序：提供活动报名、二维码签到、个人培养信息查询」；`§3.6.8`「成员在微信小程序提交请假申请」） | 无小程序代码。`tb_sign_record.device` 允许 `web/app/mini`（`sql/tb_sign_record.sql:8`）仅是字符串约定，无法证明有客户端在写 |
| **二维码内容与刷新机制** | 文档 §3.6.7「二维码包含：」「二维码默认：」两句**悬空**（后面直接是"60秒自动刷新一次"）。只知"60 秒 + 可配置 + 防截图"，不知载体（token? 签名? 活动 id?）、不知过期语义、不知"自动刷新"是前端轮询还是后端下发 |
| **`§3.5.2` 党员发展阶段权威清单** | 该小节只有标题 + 「系统统一采用以下发展阶段。」+「系统任何地方均引用统一阶段，不允许自定义。」，**清单缺失**（下一段即 §3.5.3）。只能从 §1.2/§2.5（6 项）与 §tb_member（4 项）反推，而两者互相冲突（§2.2） |
| **`§3.5.10` 电子培养档案的内容项** | 「例如：」后为空 → 不知档案包含哪些章节、版式、是否需要盖章位。只能确定"自动生成 + 导出 PDF + 方便打印" |
| **`§3.6.6` 活动详情页 / `§3.6.9` 统计项 / `§3.6.11` 成员活动记录项** | 三节均为「进入…后：」「系统自动生成：」「每位成员自动生成：」+ 空 → 字段级要求全部缺失 |
| **`§3.6.5` 「后台自动判断活动状态」的状态机** | 只知"自动判断"，不知状态取值与迁移条件。实现侧 `status 0/1/2` 的语义**仓内也无定义**（`README.md:291` 只说"取值域不同"；`ActivitySaveRequest.java:42` 只说"0/1/2"）→ 双方都缺，无法对齐 |
| **学院 / 专业 / 班级主数据** | 文档 §3.4.2 要求"不存在→提示管理员创建学院/专业/班级"，隐含三张主数据表；实现 21 表中**没有**，三列是 `tb_user_detail` 上的自由文本。是"文档多要了表"还是"实现在别处维护"，仓内无据 |
| **CAS 真实属性与账号开通流程** | `SsoServiceImpl.java:99-104` 尝试 `loginid`/`workcode`/`user` 三个键，说明对接方字段名尚未定；文档只写"学生：学号 / 教师：工号"，无一字段映射表 |
| **`tb_branch.secretary_id` 与 `branch_admin` 角色的一致性** | 实现把"管辖范围"定义为 `branch.secretary_id = 当前用户`（`IdentityChangeServiceImpl.java:200-205`），但**没有任何约束保证**"秘书一定有 branch_admin 角色"或反之。文档亦未说"支委=支部书记"。这是实现自设的隐式约定（`GLOSSARY.md:9` 只解释"秘书=审阅人"），无法判定是否为需求本意 |
| **ES 在生产是否可用** | `AdminOperationLogServiceImpl.java:30-31` 明写"ES 不可用/索引未就绪时抛出 IllegalStateException"；本地是否配置 ES 属部署事实，仓内无法判定 → 操作日志模块（#10）的"已实现"程度依赖部署 |
| **`tb_activity_detail.location` 用 `polygon` 的意图** | 签到落 `point`（`sql/tb_sign_record.sql:9`），活动地点落 `polygon`；`WktPointTypeHandler.java:22` 注释提到"地理围栏…再升级到 4326"。文档**从未要求地理围栏**（§2.7）。是历史选型还是为围栏预留，仓内无决策记录（`docs/adr/` 只有 `.gitkeep`，**零 ADR**） |

---

## 6. 附：矩阵计数与高置信度引用索引

**计数（以表内状态列为准，逐行核对）**：43 行 = **FULLY 3** {#12, #24, #29} ／ **PARTIALLY 19** {#3 #4 #6 #7 #10 #11 #13 #15 #20 #21 #22 #23 #25 #28 #35 #40 #41 #42 #43} ／ **NOT 21** {#1 #2 #5 #8 #9 #14 #16 #17 #18 #19 #26 #27 #30 #31 #32 #33 #34 #36 #37 #38 #39}。

> 说明：#35「数据统计」与 #36「新闻公告」编号相邻但状态不同（NOT / PARTIALLY），勿混。若把反向项 #27「地理围栏」剔除（文档从未要求，见 §2 与 #27 行），则为 FULLY 3 / PARTIALLY 19 / NOT 20（42 行）。

关键引用速查：

- 契约规模：`openapi.yaml` 78 path / 96 op；`README.md:176`；`GLOSSARY.md:15`（陈旧，写 81）
- 表规模：`sql/databases.sql:1-64`（21 表）；`sql/er-diagram.md:5`（`tb_resource` 未采用）
- 鉴权总闸：`config/SecurityConfig.java:54-58`；角色装配 `filter/JwtAuthFilter.java:33-39`
- 支委管辖锚点：`service/Impl/IdentityChangeServiceImpl.java:200-205`；`service/Impl/StudyRecordServiceImpl.java:216-218`
- 超管独占：`api/controller/AdminController.java:35`；`AdminSignRecordController.java:24`；`AdminActivitySignupController.java:23`
- 身份两阶段：`api/controller/IdentityChangeController.java:40,49,59,73`；`service/Impl/IdentityChangeServiceImpl.java:50-125`；`sql/tb_identity_change.sql`
- 签到 5 项校验（缺参与范围）：`service/Impl/SignRecordServiceImpl.java:36-83`
- 报名 3 维校验：`service/Impl/ActivitySignupServiceImpl.java:124-153`；描述陈旧处 `api/controller/ActivitySignupController.java:31`
- 参与范围无写入口：`api/dto/ActivitySaveRequest.java`（0 命中 target）；`repository/ActivityIdentityMapper.java`（仅被 `ActivitySignupServiceImpl.java:33` 使用）；唯一写入 `scripts/e2e-acceptance.sh:128`
- 活动物理删除：`service/Impl/AdminActivityServiceImpl.java:126-132`；FK 无级联 `sql/tb_sign_record.sql:11-13`、`sql/tb_activity_signup.sql:5-9`
- 附件白名单：`service/Impl/LocalFileStorageServiceImpl.java:34-35`；`api/controller/UploadController.java:26-31`
- 死代码/死表：`api/dto/MenuVo.java`（零引用）、`api/dto/CurrentUserResponse.java`（零引用）、`repository/OperationMapper.java`（零注入）、`model/entity/NoticeTop.java` + `repository/NoticeTopMapper.java`（零引用，置顶未实现）、`tb_operation`（从未写入）
- 密码与种子：`sql/tb_user.sql`（`default md5('123456')`）；`service/runner/InitSuperAdminRunner.java:33-40`；`service/Impl/AdminUserServiceImpl.java:50`
- 权限名不鉴权：`service/Impl/AdminRoleServiceImpl.java:23,29-37`
- 文档侧硬事实：`word/media` 不存在、`<w:drawing>`=0、`<w:pict>`=0；995 非空段落 / 19 表 / 无 3.7 节 / 163 个冒号结尾段落
