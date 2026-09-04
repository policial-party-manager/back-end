package sicau.policialPartyManager.service;

import jakarta.validation.Valid;
import sicau.policialPartyManager.api.dto.LoginRequest;
import sicau.policialPartyManager.api.dto.LoginResponse;
import sicau.policialPartyManager.api.dto.MenuVo;

import java.util.List;

public interface AuthService {
    LoginResponse loginUsernamePassword(@Valid LoginRequest request);

    LoginResponse loginEmail(@Valid LoginRequest request);

    String getUserRole(Long userId);

    List<MenuVo> buildMenus(String role);
}
