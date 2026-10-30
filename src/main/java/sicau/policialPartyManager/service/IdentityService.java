package sicau.policialPartyManager.service;

import sicau.policialPartyManager.api.dto.IdentitySaveRequest;
import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.model.entity.Identity;

/**
 * 政治身份字典服务（#44）。
 * <p>
 * 名称全表唯一；被用户当前身份或变更单引用时不可删除（可读 400）。
 * role_id 仅展示映射，不参与鉴权。
 */
public interface IdentityService {

    /** 新增身份；重名 400 */
    Identity create(IdentitySaveRequest request);

    /** 编辑身份；重名（不含自身）或不存在 400 */
    Identity update(Long id, IdentitySaveRequest request);

    /** 删除身份；被 tb_user_detail.identity_id 或变更单引用时拒绝 */
    void delete(Long id);

    /** 字典列表（登录可读，发起变更时选择目标身份用） */
    PageResult<Identity> page(long page, long size);
}
