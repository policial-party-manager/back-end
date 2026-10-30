package sicau.policialPartyManager.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sicau.policialPartyManager.model.entity.Branch;
import sicau.policialPartyManager.repository.BranchMapper;
import sicau.policialPartyManager.service.BranchService;

import java.util.List;

/**
 * 已废弃：仅服务于 {@code /api/v2/branch}，观察期后随控制器与接口一并删除。
 *
 * @deprecated 后台支部能力见 {@code AdminBranchServiceImpl}
 */
@Deprecated
@Service
@RequiredArgsConstructor
public class BranchServiceImpl implements BranchService {

    private final BranchMapper branchMapper;

    @Override
    public List<Branch> listAll() {
        return branchMapper.selectList(
                new LambdaQueryWrapper<Branch>().eq(Branch::getStatus, 1));
    }

    @Override
    public Branch getById(Long id) {
        return branchMapper.selectById(id);
    }

    @Override
    public void save(Branch branch) {
        // 不依赖 DDL 默认值：status 语义为 1 = 正常 / 0 = 已删除，缺省按正常处理
        if (branch.getStatus() == null) {
            branch.setStatus(1);
        }
        branchMapper.insert(branch);
    }

    @Override
    public void update(Branch branch) {
        branchMapper.updateById(branch);
    }

    @Override
    public void delete(Long id) {
        Branch branch = branchMapper.selectById(id);
        if (branch != null) {
            branch.setStatus(0);
            branchMapper.updateById(branch);
        }
    }
}
