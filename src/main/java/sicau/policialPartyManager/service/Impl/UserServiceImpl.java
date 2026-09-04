package sicau.policialPartyManager.service.Impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sicau.policialPartyManager.model.entity.Branch;
import sicau.policialPartyManager.model.entity.UserDetail;
import sicau.policialPartyManager.model.records.User;
import sicau.policialPartyManager.repository.BranchMapper;
import sicau.policialPartyManager.repository.UserDetailMapper;
import sicau.policialPartyManager.repository.UserMapper;
import sicau.policialPartyManager.service.UserService;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 个人中心服务（当前用户资料查看/编辑），同时保证 UserController 可装配、应用可启动。
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserDetailMapper userDetailMapper;
    private final BranchMapper branchMapper;

    @Override
    public Object profile(User user) {
        Long userId = user.userId();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", userId);
        data.put("username", user.username());

        sicau.policialPartyManager.model.entity.User entity = userMapper.selectById(userId);
        data.put("status", entity == null ? null : entity.getStatus());

        UserDetail detail = userDetailMapper.selectById(userId);
        if (detail != null) {
            data.put("realName", detail.getName());
            data.put("studentId", detail.getStudentId());
            data.put("phone", detail.getPhone());
            data.put("email", detail.getEmail());
            data.put("identityCardNumber", detail.getIdentityCardNumber());
            data.put("branchId", detail.getBranchId());
            if (detail.getBranchId() != null) {
                Branch branch = branchMapper.selectById(detail.getBranchId());
                data.put("branchName", branch == null ? null : branch.getBranchName());
            }
        } else {
            data.put("realName", null);
            data.put("studentId", null);
            data.put("phone", null);
            data.put("email", null);
            data.put("identityCardNumber", null);
            data.put("branchId", null);
        }
        return data;
    }

    @Override
    public void updateProfile(Map<String, Object> body, User user) {
        Long userId = user.userId();
        UserDetail detail = userDetailMapper.selectById(userId);
        if (detail == null) {
            detail = new UserDetail();
            detail.setUserId(userId);
        }
        String name = asString(body.get("name"));
        if (name != null) {
            detail.setName(name);
        }
        String studentId = asString(body.get("studentId"));
        if (studentId != null) {
            detail.setStudentId(studentId);
        }
        String phone = asString(body.get("phone"));
        if (phone != null) {
            detail.setPhone(phone);
        }
        String email = asString(body.get("email"));
        if (email != null) {
            detail.setEmail(email);
        }
        String identityCardNumber = asString(body.get("identityCardNumber"));
        if (identityCardNumber != null) {
            detail.setIdentityCardNumber(identityCardNumber);
        }
        Object branchId = body.get("branchId");
        if (branchId instanceof Number n) {
            detail.setBranchId(n.longValue());
        }
        if (userDetailMapper.selectById(userId) == null) {
            userDetailMapper.insert(detail);
        } else {
            userDetailMapper.updateById(detail);
        }
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? null : s;
    }
}
