package sicau.policialPartyManager.service;

import org.springframework.web.multipart.MultipartFile;
import sicau.policialPartyManager.api.dto.ImportResult;
import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.model.entity.Branch;

import java.util.List;

public interface AdminBranchService {

    /** 分页搜索支部：keyword 匹配支部名称/学院 */
    PageResult<Branch> pageBranches(long page, long size, String keyword);

    Branch getBranch(Long id);

    void saveBranch(Branch branch);

    void updateBranch(Long id, Branch branch);

    /** 软删除（status=0） */
    void deleteBranch(Long id);

    /** 启用中的支部下拉列表 */
    List<Branch> listBranchOptions();

    /** Excel 批量导入支部（列：支部名称/所属学院/简介） */
    ImportResult importBranches(MultipartFile file);

    /** 生成支部导入模板（.xlsx） */
    byte[] template();
}
