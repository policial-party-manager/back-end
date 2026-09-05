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
import java.util.function.Consumer;

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
        putDetailText(data, "realName", detail == null ? null : detail.getName());
        putDetailText(data, "studentId", detail == null ? null : detail.getStudentId());
        putDetailText(data, "phone", detail == null ? null : detail.getPhone());
        putDetailText(data, "email", detail == null ? null : detail.getEmail());
        putDetailText(data, "identityCardNumber", detail == null ? null : detail.getIdentityCardNumber());
        putDetailText(data, "gender", detail == null ? null : detail.getGender());
        putDetailText(data, "college", detail == null ? null : detail.getCollege());
        putDetailText(data, "grade", detail == null ? null : detail.getGrade());
        putDetailText(data, "major", detail == null ? null : detail.getMajor());
        putDetailText(data, "className", detail == null ? null : detail.getClassName());
        putDetailText(data, "contactPerson", detail == null ? null : detail.getContactPerson());
        putDetailText(data, "remark", detail == null ? null : detail.getRemark());
        data.put("branchId", detail == null ? null : detail.getBranchId());
        data.put("branchName", null);
        if (detail != null && detail.getBranchId() != null) {
            Branch branch = branchMapper.selectById(detail.getBranchId());
            data.put("branchName", branch == null ? null : branch.getBranchName());
        }
        return data;
    }

    @Override
    public void updateProfile(Map<String, Object> body, User user) {
        Long userId = user.userId();
        UserDetail detail = userDetailMapper.selectById(userId);
        boolean existed = detail != null;
        if (!existed) {
            detail = new UserDetail();
            detail.setUserId(userId);
        }

        Map<String, Consumer<String>> setters = Map.ofEntries(
                Map.entry("realName", detail::setName),
                Map.entry("studentId", detail::setStudentId),
                Map.entry("phone", detail::setPhone),
                Map.entry("email", detail::setEmail),
                Map.entry("identityCardNumber", detail::setIdentityCardNumber),
                Map.entry("gender", detail::setGender),
                Map.entry("college", detail::setCollege),
                Map.entry("grade", detail::setGrade),
                Map.entry("major", detail::setMajor),
                Map.entry("className", detail::setClassName),
                Map.entry("contactPerson", detail::setContactPerson),
                Map.entry("remark", detail::setRemark)
        );
        setters.forEach((key, setter) -> {
            String value = asString(body.get(key));
            if (value != null) {
                setter.accept(value);
            }
        });

        Object branchId = body.get("branchId");
        if (branchId instanceof Number n) {
            detail.setBranchId(n.longValue());
        }
        if (existed) {
            userDetailMapper.updateById(detail);
        } else {
            userDetailMapper.insert(detail);
        }
    }

    private void putDetailText(Map<String, Object> data, String key, String value) {
        data.put(key, value);
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? null : s;
    }
}
