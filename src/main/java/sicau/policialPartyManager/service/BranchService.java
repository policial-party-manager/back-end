package sicau.policialPartyManager.service;

import sicau.policialPartyManager.model.entity.Branch;

import java.util.List;

/**
 * 党支部服务（v2 旧接口专用）。
 *
 * @deprecated 仅服务于已废弃的 {@code /api/v2/branch}；后台能力请用
 *             {@code AdminBranchService}（分页/搜索/校验/导入），观察期后随
 *             {@code BranchController} 一并删除。
 */
@Deprecated
public interface BranchService {

    List<Branch> listAll();

    Branch getById(Long id);

    void save(Branch branch);

    void update(Branch branch);

    void delete(Long id);
}
