package sicau.policialPartyManager.service;

import org.springframework.web.multipart.MultipartFile;
import sicau.policialPartyManager.api.dto.ImportResult;
import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.api.dto.UserSaveRequest;
import sicau.policialPartyManager.api.dto.UserVo;

public interface AdminUserService {

    /** 分页搜索用户：keyword 匹配 姓名/用户名/学号/手机/邮箱；可按支部/角色/状态过滤 */
    PageResult<UserVo> pageUsers(long page, long size, String keyword, Long branchId, Long roleId, Integer status);

    /** 用户详情（含角色、支部） */
    UserVo getUser(Long id);

    /** 新增用户（用户名唯一；密码默认 123456；默认角色普通成员） */
    void createUser(UserSaveRequest request);

    /** 编辑用户资料/角色/支部；用户名不允许修改 */
    void updateUser(Long id, UserSaveRequest request);

    /** 启用/停用（status=1/2） */
    void updateStatus(Long id, Integer status);

    /** 重置密码（newPassword 为空则重置为 123456） */
    void resetPassword(Long id, String newPassword);

    /** Excel 批量导入用户（列：学号/姓名/手机号/邮箱/所属支部/角色） */
    ImportResult importUsers(MultipartFile file);

    /** 生成用户导入模板（.xlsx） */
    byte[] template();
}
