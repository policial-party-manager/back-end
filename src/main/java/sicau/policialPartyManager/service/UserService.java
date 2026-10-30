package sicau.policialPartyManager.service;

import sicau.policialPartyManager.api.dto.UserProfileUpdateRequest;
import sicau.policialPartyManager.api.dto.UserProfileVo;
import sicau.policialPartyManager.model.records.User;

/**
 * 个人中心服务（当前用户资料查看/编辑）。
 */
public interface UserService {

    /** 当前用户资料（账号 + 成员档案 + 所属支部） */
    UserProfileVo profile(User user);

    /**
     * 编辑当前用户资料（部分更新：请求中为 null/空白的字段保持原值）。
     * 白名单见 {@link UserProfileUpdateRequest}；支部归属不在白名单内（由管理员维护）。
     */
    void updateProfile(UserProfileUpdateRequest request, User user);
}
