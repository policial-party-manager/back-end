package sicau.policialPartyManager.service.Impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sicau.policialPartyManager.api.dto.UserProfileUpdateRequest;
import sicau.policialPartyManager.api.dto.UserProfileVo;
import sicau.policialPartyManager.model.entity.Branch;
import sicau.policialPartyManager.model.entity.UserDetail;
import sicau.policialPartyManager.model.records.User;
import sicau.policialPartyManager.repository.BranchMapper;
import sicau.policialPartyManager.repository.UserDetailMapper;
import sicau.policialPartyManager.repository.UserMapper;
import sicau.policialPartyManager.service.UserService;

/**
 * 个人中心服务：类型化出入参，部分更新语义与原 Map 实现一致。
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserDetailMapper userDetailMapper;
    private final BranchMapper branchMapper;

    @Override
    public UserProfileVo profile(User user) {
        Long userId = user.userId();
        UserProfileVo vo = new UserProfileVo();
        vo.setId(userId);
        vo.setUsername(user.username());

        sicau.policialPartyManager.model.entity.User entity = userMapper.selectById(userId);
        vo.setStatus(entity == null ? null : entity.getStatus());

        UserDetail detail = userDetailMapper.selectById(userId);
        if (detail != null) {
            vo.setRealName(detail.getName());
            vo.setStudentId(detail.getStudentId());
            vo.setPhone(detail.getPhone());
            vo.setEmail(detail.getEmail());
            vo.setIdentityCardNumber(detail.getIdentityCardNumber());
            vo.setGender(detail.getGender());
            vo.setCollege(detail.getCollege());
            vo.setGrade(detail.getGrade());
            vo.setMajor(detail.getMajor());
            vo.setClassName(detail.getClassName());
            vo.setContactPerson(detail.getContactPerson());
            vo.setRemark(detail.getRemark());
            vo.setBranchId(detail.getBranchId());
            if (detail.getBranchId() != null) {
                Branch branch = branchMapper.selectById(detail.getBranchId());
                vo.setBranchName(branch == null ? null : branch.getBranchName());
            }
        }
        return vo;
    }

    @Override
    public void updateProfile(UserProfileUpdateRequest request, User user) {
        Long userId = user.userId();
        UserDetail detail = userDetailMapper.selectById(userId);
        boolean existed = detail != null;
        if (!existed) {
            detail = new UserDetail();
            detail.setUserId(userId);
        }

        // 部分更新：仅覆盖请求中实际给出的非空白字段（与原 Map 实现一致）
        String realName = trimToNull(request.getRealName());
        if (realName != null) {
            detail.setName(realName);
        }
        String studentId = trimToNull(request.getStudentId());
        if (studentId != null) {
            detail.setStudentId(studentId);
        }
        String identityCardNumber = trimToNull(request.getIdentityCardNumber());
        if (identityCardNumber != null) {
            detail.setIdentityCardNumber(identityCardNumber);
        }
        String phone = trimToNull(request.getPhone());
        if (phone != null) {
            detail.setPhone(phone);
        }
        String email = trimToNull(request.getEmail());
        if (email != null) {
            detail.setEmail(email);
        }
        String gender = trimToNull(request.getGender());
        if (gender != null) {
            detail.setGender(gender);
        }
        String college = trimToNull(request.getCollege());
        if (college != null) {
            detail.setCollege(college);
        }
        String grade = trimToNull(request.getGrade());
        if (grade != null) {
            detail.setGrade(grade);
        }
        String major = trimToNull(request.getMajor());
        if (major != null) {
            detail.setMajor(major);
        }
        String className = trimToNull(request.getClassName());
        if (className != null) {
            detail.setClassName(className);
        }
        String contactPerson = trimToNull(request.getContactPerson());
        if (contactPerson != null) {
            detail.setContactPerson(contactPerson);
        }
        String remark = trimToNull(request.getRemark());
        if (remark != null) {
            detail.setRemark(remark);
        }

        if (existed) {
            userDetailMapper.updateById(detail);
        } else {
            userDetailMapper.insert(detail);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
