package sicau.policialPartyManager.service;

import sicau.policialPartyManager.model.records.User;

import java.util.Map;

public interface UserService {
    Object profile(User user);

    void updateProfile(Map<String, Object> body, User user);
}
