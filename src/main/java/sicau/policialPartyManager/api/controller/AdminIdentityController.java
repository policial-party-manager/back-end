package sicau.policialPartyManager.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sicau.policialPartyManager.api.dto.IdentitySaveRequest;
import sicau.policialPartyManager.api.dto.Result;
import sicau.policialPartyManager.model.entity.Identity;
import sicau.policialPartyManager.service.IdentityService;

/**
 * 政治身份字典维护（#44）：仅超级管理员。读列表在内容侧（登录即可）。
 */
@Tag(name = "管理员后台-身份字典", description = "政治身份字典的增删改（超管）")
@RestController
@RequestMapping("/api/v4/admin/identities")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminIdentityController {

    private final IdentityService identityService;

    @Operation(summary = "新增身份",
            description = "identityName 全表唯一、level 1~6；role_id 仅展示映射不参与鉴权")
    @PostMapping
    public Result<Identity> create(@Valid @RequestBody IdentitySaveRequest request) {
        return Result.ok(identityService.create(request));
    }

    @Operation(summary = "编辑身份", description = "重名（不含自身）拒绝")
    @PutMapping("/{id}")
    public Result<Identity> update(@PathVariable Long id, @Valid @RequestBody IdentitySaveRequest request) {
        return Result.ok(identityService.update(id, request));
    }

    @Operation(summary = "删除身份",
            description = "被用户当前身份或变更单引用时拒绝（可读 400）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        identityService.delete(id);
        return Result.ok();
    }
}
