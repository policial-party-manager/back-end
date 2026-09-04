package sicau.policialPartyManager.service;

import sicau.policialPartyManager.filter.JwtAuthFilter;

import java.util.Map;

public interface UserService {
    Object profile(JwtAuthFilter.TokenUser user);

    void updateProfile(Map<String, Object> body, JwtAuthFilter.TokenUser user);
}
