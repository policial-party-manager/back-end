package sicau.policialPartyManager.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sicau.policialPartyManager.api.dto.*;
import sicau.policialPartyManager.config.CurrentUser;
import sicau.policialPartyManager.log.OperationLog;
import sicau.policialPartyManager.model.entity.*;
import sicau.policialPartyManager.model.records.User;
import sicau.policialPartyManager.service.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 管理员后台（对前端统一的 admin 入口）。
 * <p>
 * 全部对外接口集中在当前控制器，按模块分节：
 * 用户管理 / 党支部管理 / 权限（角色）管理 / 活动管理 / 新闻管理 / 公告管理。
 * 仅超级管理员可访问；支部管理员的细分权限后续在角色粒度上扩展。
 */
@Tag(name = "管理员后台", description = "用户、党支部、角色权限、活动、新闻、公告的后台管理及 Excel 批量导入")
@RestController
@RequestMapping("/api/v4/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminUserService adminUserService;
    private final AdminBranchService adminBranchService;
    private final AdminRoleService adminRoleService;
    private final AdminActivityService adminActivityService;
    private final AdminNewsService adminNewsService;
    private final AdminNoticeService adminNoticeService;
    private final AdminOperationLogService adminOperationLogService;

    // =====================================================================
    // 一、用户管理
    // =====================================================================

    @Operation(summary = "用户分页列表", description = "keyword 匹配姓名/用户名/学号/手机/邮箱，可按支部/角色/状态过滤")
    @GetMapping("/users/page")
    public Result<PageResult<UserVo>> pageUsers(@RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) Long branchId,
                                                @RequestParam(required = false) Long roleId,
                                                @RequestParam(required = false) Integer status) {
        return Result.ok(adminUserService.pageUsers(page, size, keyword, branchId, roleId, status));
    }

    @Operation(summary = "用户详情")
    @GetMapping("/users/{id}")
    public Result<UserVo> getUser(@PathVariable Long id) {
        return Result.ok(adminUserService.getUser(id));
    }

    @Operation(summary = "新增用户", description = "用户名唯一；密码缺省 123456（填写须 6-16 位字母/数字/下划线）；角色缺省普通成员")
    @PostMapping("/users")
    public Result<Void> createUser(@Validated({ValidationGroups.Create.class, jakarta.validation.groups.Default.class})
                                   @RequestBody UserSaveRequest request) {
        adminUserService.createUser(request);
        return Result.ok();
    }

    @Operation(summary = "编辑用户", description = "资料/角色/支部可改，用户名不可改；支持部分更新（未传字段保留原值）")
    @PutMapping("/users/{id}")
    public Result<Void> updateUser(@PathVariable Long id, @Valid @RequestBody UserSaveRequest request) {
        adminUserService.updateUser(id, request);
        return Result.ok();
    }

    @Operation(summary = "启用/停用用户", description = "status：1 启用 / 2 停用")
    @PutMapping("/users/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id, @RequestParam Integer status) {
        adminUserService.updateStatus(id, status);
        return Result.ok();
    }

    @Operation(summary = "重置用户密码", description = "新密码须 6-16 位字母/数字/下划线")
    @PutMapping("/users/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        adminUserService.resetPassword(id, request.getPassword());
        return Result.ok();
    }

    @Operation(summary = "下载用户导入模板", description = "列：学号、姓名、手机号、邮箱、所属支部、角色")
    @GetMapping("/users/template")
    public void downloadUserTemplate(HttpServletResponse response) throws IOException {
        writeXlsx(response, adminUserService.template(), "用户导入模板.xlsx");
    }

    @Operation(summary = "Excel 批量导入用户", description = "返回成功/失败计数与逐行失败原因")
    @PostMapping(value = "/users/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<ImportResult> importUsers(@RequestParam("file") MultipartFile file) throws IOException {
        return Result.ok(adminUserService.importUsers(file));
    }

    // =====================================================================
    // 二、党支部管理
    // =====================================================================

    @Operation(summary = "支部分页列表", description = "keyword 匹配支部名称/学院")
    @GetMapping("/branches/page")
    public Result<PageResult<Branch>> pageBranches(@RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "10") long size,
                                                   @RequestParam(required = false) String keyword) {
        return Result.ok(adminBranchService.pageBranches(page, size, keyword));
    }

    @Operation(summary = "支部详情")
    @GetMapping("/branches/{id}")
    public Result<Branch> getBranch(@PathVariable Long id) {
        return Result.ok(adminBranchService.getBranch(id));
    }

    @Operation(summary = "新增支部")
    @PostMapping("/branches")
    public Result<Void> createBranch(@RequestBody Branch branch) {
        adminBranchService.saveBranch(branch);
        return Result.ok();
    }

    @Operation(summary = "编辑支部")
    @PutMapping("/branches/{id}")
    public Result<Void> updateBranch(@PathVariable Long id, @RequestBody Branch branch) {
        adminBranchService.updateBranch(id, branch);
        return Result.ok();
    }

    @Operation(summary = "删除支部（软删除）")
    @DeleteMapping("/branches/{id}")
    public Result<Void> deleteBranch(@PathVariable Long id) {
        adminBranchService.deleteBranch(id);
        return Result.ok();
    }

    @Operation(summary = "支部下拉列表", description = "启用中的支部")
    @GetMapping("/branches/options")
    public Result<List<Branch>> branchOptions() {
        return Result.ok(adminBranchService.listBranchOptions());
    }

    @Operation(summary = "下载支部导入模板", description = "列：支部名称、所属学院、简介")
    @GetMapping("/branches/template")
    public void downloadBranchTemplate(HttpServletResponse response) throws IOException {
        writeXlsx(response, adminBranchService.template(), "党支部导入模板.xlsx");
    }

    @Operation(summary = "Excel 批量导入支部", description = "返回成功/失败计数与逐行失败原因")
    @PostMapping(value = "/branches/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<ImportResult> importBranches(@RequestParam("file") MultipartFile file) throws IOException {
        return Result.ok(adminBranchService.importBranches(file));
    }

    // =====================================================================
    // 三、权限管理（角色）
    // =====================================================================

    @Operation(summary = "角色列表", description = "含权限名与使用人数，可用于下拉")
    @GetMapping("/roles")
    public Result<List<RoleVo>> listRoles() {
        return Result.ok(adminRoleService.listRoles());
    }

    @Operation(summary = "角色详情")
    @GetMapping("/roles/{id}")
    public Result<RoleVo> getRole(@PathVariable Long id) {
        return Result.ok(adminRoleService.getRole(id));
    }

    @Operation(summary = "新增角色", description = "roleCode 规范化存储（ROLE_ 前缀大写），并分配权限名")
    @PostMapping("/roles")
    public Result<Void> createRole(@Validated(ValidationGroups.Create.class) @RequestBody RoleSaveRequest request) {
        adminRoleService.saveRole(request);
        return Result.ok();
    }

    @Operation(summary = "编辑角色", description = "roleCode 可空表示沿用原名；权限名分配请走 /roles/{id}/permissions")
    @PutMapping("/roles/{id}")
    public Result<Void> updateRole(@PathVariable Long id, @Valid @RequestBody RoleSaveRequest request) {
        adminRoleService.updateRole(id, request);
        return Result.ok();
    }

    @Operation(summary = "删除角色", description = "仍有用户使用该角色时拒绝删除")
    @DeleteMapping("/roles/{id}")
    public Result<Void> deleteRole(@PathVariable Long id) {
        adminRoleService.deleteRole(id);
        return Result.ok();
    }

    @Operation(summary = "内置权限点字典", description = "可分配给角色的权限名集合（现阶段角色级鉴权，映射用于维护/预留）")
    @GetMapping("/roles/permission-names")
    public Result<List<String>> permissionNames() {
        return Result.ok(adminRoleService.listPermissionNames());
    }

    @Operation(summary = "分配角色权限", description = "permissionNames 全量覆盖")
    @PutMapping("/roles/{id}/permissions")
    public Result<Void> assignPermissions(@PathVariable Long id, @RequestBody RoleSaveRequest request) {
        adminRoleService.assignPermissions(id, request.getPermissionNames());
        return Result.ok();
    }

    // =====================================================================
    // 四、活动管理（含活动类型）
    // =====================================================================

    @Operation(summary = "活动分页列表", description = "keyword 匹配标题/简介，可按支部/类型/状态过滤")
    @GetMapping("/activities/page")
    public Result<PageResult<ActivityVo>> pageActivities(@RequestParam(defaultValue = "1") long page,
                                                         @RequestParam(defaultValue = "10") long size,
                                                         @RequestParam(required = false) String keyword,
                                                         @RequestParam(required = false) Long branchId,
                                                         @RequestParam(required = false) Integer type,
                                                         @RequestParam(required = false) Integer status) {
        return Result.ok(adminActivityService.pageActivities(page, size, keyword, branchId, type, status));
    }

    @Operation(summary = "活动详情")
    @GetMapping("/activities/{id}")
    public Result<ActivityVo> getActivity(@PathVariable Long id) {
        return Result.ok(adminActivityService.getActivity(id));
    }

    @Operation(summary = "新增活动", description = "主体 + 详情一起保存，标题必填")
    @PostMapping("/activities")
    public Result<Void> createActivity(@Valid @RequestBody ActivitySaveRequest request, @CurrentUser User user) {
        adminActivityService.createActivity(request, user.userId());
        return Result.ok();
    }

    @Operation(summary = "编辑活动")
    @PutMapping("/activities/{id}")
    public Result<Void> updateActivity(@PathVariable Long id, @Valid @RequestBody ActivitySaveRequest request,
                                       @CurrentUser User user) {
        adminActivityService.updateActivity(id, request, user.userId());
        return Result.ok();
    }

    @Operation(summary = "删除活动", description = "连同活动详情一起删除")
    @DeleteMapping("/activities/{id}")
    public Result<Void> deleteActivity(@PathVariable Long id) {
        adminActivityService.deleteActivity(id);
        return Result.ok();
    }

    @Operation(summary = "调整活动状态", description = "status：0/1/2")
    @PutMapping("/activities/{id}/status")
    public Result<Void> updateActivityStatus(@PathVariable Long id, @RequestParam Integer status) {
        adminActivityService.updateActivityStatus(id, status);
        return Result.ok();
    }

    @Operation(summary = "活动类型列表")
    @GetMapping("/activity-types")
    public Result<List<ActivityType>> listActivityTypes() {
        return Result.ok(adminActivityService.listActivityTypes());
    }

    @Operation(summary = "新增活动类型")
    @PostMapping("/activity-types")
    public Result<Void> createActivityType(@RequestBody ActivityType type) {
        adminActivityService.saveActivityType(type);
        return Result.ok();
    }

    @Operation(summary = "编辑活动类型")
    @PutMapping("/activity-types/{id}")
    public Result<Void> updateActivityType(@PathVariable Long id, @RequestBody ActivityType type) {
        adminActivityService.updateActivityType(id, type);
        return Result.ok();
    }

    @Operation(summary = "删除活动类型", description = "已被活动引用时拒绝删除")
    @DeleteMapping("/activity-types/{id}")
    public Result<Void> deleteActivityType(@PathVariable Long id) {
        adminActivityService.deleteActivityType(id);
        return Result.ok();
    }

    // =====================================================================
    // 五、新闻管理
    // =====================================================================

    @Operation(summary = "新闻分页列表", description = "keyword 匹配标题/正文，可按类型/状态过滤")
    @GetMapping("/news/page")
    public Result<PageResult<News>> pageNews(@RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "10") long size,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) String type,
                                             @RequestParam(required = false) Integer status) {
        return Result.ok(adminNewsService.pageNews(page, size, keyword, type, status));
    }

    @Operation(summary = "新闻详情")
    @GetMapping("/news/{id}")
    public Result<News> getNews(@PathVariable Long id) {
        return Result.ok(adminNewsService.getNews(id));
    }

    @Operation(summary = "新增新闻", description = "作者为当前登录用户")
    @PostMapping("/news")
    public Result<Void> createNews(@RequestBody News news, @CurrentUser User user) {
        adminNewsService.createNews(news, user.userId());
        return Result.ok();
    }

    @Operation(summary = "编辑新闻")
    @PutMapping("/news/{id}")
    public Result<Void> updateNews(@PathVariable Long id, @RequestBody News news, @CurrentUser User user) {
        adminNewsService.updateNews(id, news, user.userId());
        return Result.ok();
    }

    @Operation(summary = "删除新闻")
    @DeleteMapping("/news/{id}")
    public Result<Void> deleteNews(@PathVariable Long id) {
        adminNewsService.deleteNews(id);
        return Result.ok();
    }

    @Operation(summary = "新闻发布/下线", description = "status：1 上线 / 2 下线")
    @PutMapping("/news/{id}/status")
    public Result<Void> updateNewsStatus(@PathVariable Long id, @RequestParam Integer status) {
        adminNewsService.updateStatus(id, status);
        return Result.ok();
    }

    // =====================================================================
    // 六、公告管理
    // =====================================================================

    @Operation(summary = "公告分页列表", description = "keyword 匹配标题/正文，可按状态过滤")
    @GetMapping("/notices/page")
    public Result<PageResult<Notice>> pageNotices(@RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "10") long size,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) Integer status) {
        return Result.ok(adminNoticeService.pageNotices(page, size, keyword, status));
    }

    @Operation(summary = "公告详情")
    @GetMapping("/notices/{id}")
    public Result<Notice> getNotice(@PathVariable Long id) {
        return Result.ok(adminNoticeService.getNotice(id));
    }

    @Operation(summary = "新增公告", description = "发布者为当前登录用户，默认发布")
    @PostMapping("/notices")
    public Result<Void> createNotice(@RequestBody Notice notice, @CurrentUser User user) {
        adminNoticeService.createNotice(notice, user.userId());
        return Result.ok();
    }

    @Operation(summary = "编辑公告")
    @PutMapping("/notices/{id}")
    public Result<Void> updateNotice(@PathVariable Long id, @RequestBody Notice notice, @CurrentUser User user) {
        adminNoticeService.updateNotice(id, notice, user.userId());
        return Result.ok();
    }

    @Operation(summary = "删除公告", description = "标记删除（status=2），保留记录")
    @DeleteMapping("/notices/{id}")
    public Result<Void> deleteNotice(@PathVariable Long id) {
        adminNoticeService.deleteNotice(id);
        return Result.ok();
    }

    @Operation(summary = "发布公告", description = "status=1 且 publishTime=当前时间")
    @PutMapping("/notices/{id}/publish")
    public Result<Void> publishNotice(@PathVariable Long id) {
        adminNoticeService.publishNotice(id);
        return Result.ok();
    }

    // =====================================================================
    // 七、统一操作日志（Elasticsearch）
    // =====================================================================

    @Operation(summary = "操作日志分页检索", description = "按类型/模块/操作者/IP/成功与否/关键字/时间范围检索；时间格式 ISO-8601 或 yyyy-MM-dd HH:mm:ss")
    @GetMapping("/logs/page")
    public Result<PageResult<OperationLog>> pageLogs(@RequestParam(defaultValue = "1") long page,
                                                     @RequestParam(defaultValue = "10") long size,
                                                     @RequestParam(required = false) String logType,
                                                     @RequestParam(required = false) String module,
                                                     @RequestParam(required = false) String operatorName,
                                                     @RequestParam(required = false) String ip,
                                                     @RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) Boolean success,
                                                     @RequestParam(required = false) String errorType,
                                                     @RequestParam(required = false) String startTime,
                                                     @RequestParam(required = false) String endTime) {
        return Result.ok(adminOperationLogService.pageLogs(page, size, logType, module,
                operatorName, ip, keyword, success, errorType, startTime, endTime));
    }

    @Operation(summary = "日志类型统计", description = "按 LOGIN/LOGOUT/ERROR/OPERATION 计数")
    @GetMapping("/logs/stats")
    public Result<Map<String, Long>> logStats() {
        return Result.ok(adminOperationLogService.stats());
    }

    @Operation(summary = "清理过期日志", description = "删除保留天数（默认 90，1~730）之前的日志，返回删除条数")
    @DeleteMapping("/logs/cleanup")
    public Result<Long> cleanupLogs(@RequestParam(defaultValue = "90") int days) {
        return Result.ok(adminOperationLogService.cleanup(days));
    }

    // =====================================================================
    // 工具
    // =====================================================================

    /** 以附件形式输出 .xlsx */
    private void writeXlsx(HttpServletResponse response, byte[] bytes, String filename) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String encoded = java.net.URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
        response.getOutputStream().write(bytes);
        response.getOutputStream().flush();
    }
}
