package sicau.policialPartyManager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sicau.policialPartyManager.entity.Branch;
import sicau.policialPartyManager.repository.BranchMapper;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchMapper branchMapper;

    public List<Branch> listAll() {
        return branchMapper.selectList(
                new LambdaQueryWrapper<Branch>().eq(Branch::getStatus, 1));
    }

    public Branch getById(Long id) {
        return branchMapper.selectById(id);
    }

    public void save(Branch branch) {
        branchMapper.insert(branch);
    }

    public void update(Branch branch) {
        branchMapper.updateById(branch);
    }

    public void delete(Long id) {
        Branch branch = branchMapper.selectById(id);
        if (branch != null) {
            branch.setStatus(0);
            branchMapper.updateById(branch);
        }
    }
}
