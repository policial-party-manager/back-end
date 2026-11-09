package sicau.policialPartyManager.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sicau.policialPartyManager.api.dto.MemberProfileUpdateRequest;
import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.api.dto.Result;
import sicau.policialPartyManager.api.dto.UserVo;
import sicau.policialPartyManager.config.CurrentUser;
import sicau.policialPartyManager.model.records.User;
import sicau.policialPartyManager.service.Impl.ScopedMemberServiceImpl;

import java.nio.charset.StandardCharsets;

/**
 * 成员管理（按数据范围，支委可用）：#70。
 * <p>超管全院、支委限本支部；越权读取与真不存在同文案（不存在语义）。
 * <p>仅开放文档权限矩阵中支委持有的能力：查看、改联系方式、导出。
 * 新增/删除/改权威字段/导入 仍归超管（{@code /api/v4/admin/users/*}）。
 */
@Tag(name = "成员管理（支委）", description = "本支部成员查看、联系方式维护、名单导出")
@RestController
@RequestMapping("/api/v4/members")
@RequiredArgsConstructor
public class MemberController {

    private final ScopedMemberServiceImpl scopedMemberService;

    @Operation(summary = "成员分页（按操作者范围）",
            description = "超管全院；支委限本支部。支持 keyword 与年级/专业/学院/政治身份筛选")
    @GetMapping("/page")
    public Result<PageResult<UserVo>> page(@CurrentUser User operator,
                                           @RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String grade,
                                           @RequestParam(required = false) String major,
                                           @RequestParam(required = false) String college,
                                           @RequestParam(required = false) Long identityId,
                                           @RequestParam(required = false) Integer status) {
        return Result.ok(scopedMemberService.pageUsersScoped(
                operator, page, size, keyword, grade, major, college, identityId, status));
    }

    @Operation(summary = "成员详情（按操作者范围）", description = "越权与真不存在同文案")
    @GetMapping("/{id}")
    public Result<UserVo> detail(@CurrentUser User operator, @PathVariable Long id) {
        return Result.ok(scopedMemberService.getUserScoped(operator, id));
    }

    @Operation(summary = "修改成员联系方式",
            description = "仅手机号/邮箱/备注；部分更新，未传字段不动，空串清空")
    @PutMapping("/{id}/contact")
    public Result<Void> updateContact(@CurrentUser User operator, @PathVariable Long id,
                                      @Valid @RequestBody MemberProfileUpdateRequest request) {
        scopedMemberService.updateContactScoped(operator, id, request);
        return Result.ok();
    }

    @Operation(summary = "导出成员名单（xlsx）", description = "受与列表相同的筛选条件约束")
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@CurrentUser User operator,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) String grade,
                                         @RequestParam(required = false) String major,
                                         @RequestParam(required = false) String college,
                                         @RequestParam(required = false) Long identityId,
                                         @RequestParam(required = false) Integer status) {
        byte[] body = scopedMemberService.exportScoped(
                operator, keyword, grade, major, college, identityId, status);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("members.xlsx", StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }
}
