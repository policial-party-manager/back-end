package sicau.policialPartyManager.service;

import sicau.policialPartyManager.api.dto.IdentityChangeApplyRequest;
import sicau.policialPartyManager.api.dto.IdentityChangeVo;
import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.model.records.User;

/**
 * 身份变更单服务（#44，两阶段）。
 * <p>
 * 发起：branch_admin（限本支部）或 super_admin；fromIdentity 取目标当前身份快照（首次为 null）；
 * 同一用户仅允许一张待审单。审批：仅 super_admin；通过才写回 identity_id；
 * 驳回现身份不动。越权查看单据统一「变更单不存在」语义。
 * 硬约束：任何环节不读写 tb_user_role（身份变更与登录角色完全解耦）。
 */
public interface IdentityChangeService {

    IdentityChangeVo apply(IdentityChangeApplyRequest request, User operator);

    IdentityChangeVo approve(Long id, Boolean pass, String remark, User reviewer);

    /**
     * 单据详情。超管全量可见；支管限本支部成员的单；成员仅本人单。
     * 越权与不存在统一「变更单不存在」语义。
     */
    IdentityChangeVo detail(Long id, User viewer);

    /** 成员：我的变更单分页 */
    PageResult<IdentityChangeVo> pageMine(long page, long size, User user);

    /** 支管：本支部成员的变更单分页 */
    PageResult<IdentityChangeVo> pageForBranch(long page, long size, User operator);

    /** 超管：全局分页（可按用户/状态过滤） */
    PageResult<IdentityChangeVo> pageForAdmin(long page, long size, Long userId, Integer status);
}
