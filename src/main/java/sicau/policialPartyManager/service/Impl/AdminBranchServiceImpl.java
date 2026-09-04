package sicau.policialPartyManager.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import sicau.policialPartyManager.api.dto.ImportResult;
import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.model.entity.Branch;
import sicau.policialPartyManager.repository.BranchMapper;
import sicau.policialPartyManager.service.AdminBranchService;
import sicau.policialPartyManager.utils.ExcelUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 党支部管理（后台）：分页搜索、增删改查、Excel 批量导入。
 */
@Service
@RequiredArgsConstructor
public class AdminBranchServiceImpl implements AdminBranchService {

    private static final String[] IMPORT_HEADERS = {"支部名称", "所属学院", "简介"};
    private static final String[][] IMPORT_EXAMPLE = {
            {"第一党支部", "信息工程学院", "示例：负责学院党员日常管理与活动组织"}
    };

    private final BranchMapper branchMapper;

    @Override
    public PageResult<Branch> pageBranches(long page, long size, String keyword) {
        LambdaQueryWrapper<Branch> wrapper = new LambdaQueryWrapper<Branch>()
                .eq(Branch::getStatus, 1)
                .orderByAsc(Branch::getId);
        if (StringUtils.hasText(keyword)) {
            String k = keyword.trim();
            wrapper.and(w -> w.like(Branch::getBranchName, k).or().like(Branch::getCollege, k));
        }
        Page<Branch> result = branchMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public Branch getBranch(Long id) {
        Branch branch = branchMapper.selectById(id);
        if (branch == null || branch.getStatus() == null || branch.getStatus() != 1) {
            throw new IllegalArgumentException("支部不存在");
        }
        return branch;
    }

    @Override
    public void saveBranch(Branch branch) {
        String name = branch.getBranchName();
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("支部名称不能为空");
        }
        checkNameFree(name.trim(), null);
        branch.setId(null);
        branch.setBranchName(name.trim());
        branch.setStatus(1);
        branchMapper.insert(branch);
    }

    @Override
    public void updateBranch(Long id, Branch branch) {
        Branch existing = branchMapper.selectById(id);
        if (existing == null || existing.getStatus() == null || existing.getStatus() != 1) {
            throw new IllegalArgumentException("支部不存在");
        }
        if (StringUtils.hasText(branch.getBranchName())) {
            checkNameFree(branch.getBranchName().trim(), id);
            existing.setBranchName(branch.getBranchName().trim());
        }
        if (branch.getCollege() != null) {
            existing.setCollege(branch.getCollege());
        }
        if (branch.getSecretaryId() != null) {
            existing.setSecretaryId(branch.getSecretaryId());
        }
        if (branch.getDescription() != null) {
            existing.setDescription(branch.getDescription());
        }
        branchMapper.updateById(existing);
    }

    @Override
    public void deleteBranch(Long id) {
        Branch branch = branchMapper.selectById(id);
        if (branch == null) {
            return;
        }
        branch.setStatus(0);
        branchMapper.updateById(branch);
    }

    @Override
    public List<Branch> listBranchOptions() {
        return branchMapper.selectList(new LambdaQueryWrapper<Branch>()
                .eq(Branch::getStatus, 1).orderByAsc(Branch::getId));
    }

    @Override
    public ImportResult importBranches(MultipartFile file) {
        List<String[]> rows;
        try {
            rows = ExcelUtil.readDataRows(file, IMPORT_HEADERS);
        } catch (IOException e) {
            throw new IllegalArgumentException("Excel 解析失败：" + e.getMessage());
        }

        int success = 0;
        List<ImportResult.RowError> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);
            int rowNo = i + 2;
            try {
                String name = cell(row, 0);
                if (!StringUtils.hasText(name)) {
                    throw new IllegalArgumentException("支部名称不能为空");
                }
                checkNameFree(name.trim(), null);
                Branch branch = new Branch();
                branch.setBranchName(name.trim());
                branch.setCollege(cellOrNull(row, 1));
                branch.setDescription(cellOrNull(row, 2));
                branch.setStatus(1);
                branchMapper.insert(branch);
                success++;
            } catch (RuntimeException e) {
                errors.add(new ImportResult.RowError(rowNo, e.getMessage()));
            }
        }
        return ImportResult.of(rows.size(), success, errors);
    }

    @Override
    public byte[] template() {
        try {
            return ExcelUtil.createTemplate("党支部导入", IMPORT_HEADERS, IMPORT_EXAMPLE);
        } catch (IOException e) {
            throw new IllegalStateException("生成导入模板失败", e);
        }
    }

    /** 同名（任意状态）视为冲突，防止重复 */
    private void checkNameFree(String name, Long excludeId) {
        LambdaQueryWrapper<Branch> wrapper = new LambdaQueryWrapper<Branch>()
                .eq(Branch::getBranchName, name);
        if (excludeId != null) {
            wrapper.ne(Branch::getId, excludeId);
        }
        Long count = branchMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new IllegalArgumentException("支部名称已存在");
        }
    }

    private String cell(String[] row, int index) {
        return index < row.length && row[index] != null ? row[index].trim() : "";
    }

    private String cellOrNull(String[] row, int index) {
        String value = cell(row, index);
        return value.isEmpty() ? null : value;
    }
}
