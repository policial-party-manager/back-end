package sicau.policialPartyManager.service;

import sicau.policialPartyManager.model.records.User;

/**
 * 支委数据范围判定（#70 的统一判据）。
 * <p>「哪个支部」以成员所属支部（{@code tb_user_detail.branch_id}）为准，
 * 而不是「谁是该支部的书记」——后者只用于派定思想汇报审阅人。
 * <p>超级管理员不受限；支委限本支部；普通成员无后台权限。
 */
public interface BranchScopeService {

    /** 是否超级管理员。 */
    boolean isSuperAdmin(User operator);

    /** 是否支委。 */
    boolean isBranchAdmin(User operator);

    /**
     * 操作者所属支部 id；非支委或未挂支部时返回 {@code null}。
     * <p>支委的支部取自其本人 {@code tb_user_detail.branch_id}。
     */
    Long branchIdOf(User operator);

    /**
     * 断言操作者可管理目标支部；不可管理时抛出「不存在」语义的异常。
     * <p>超级管理员放行任意支部；支委仅放行本支部；其余一律拒绝。
     */
    void requireManageableBranch(User operator, Long targetBranchId);

    /**
     * 断言操作者可管理目标成员（按成员所属支部判定）。
     * <p>成员不存在或不在操作者范围内，统一抛出「用户不存在」，不泄露存在性。
     */
    void requireManageableUser(User operator, Long targetUserId);
}
