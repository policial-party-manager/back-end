package sicau.policialPartyManager.service;

import jakarta.validation.Valid;
import sicau.policialPartyManager.dto.LoginRequest;
import sicau.policialPartyManager.dto.LoginResponse;
import sicau.policialPartyManager.dto.MenuVo;

import java.util.List;

public interface AuthService {
    LoginResponse login(@Valid LoginRequest request);

    String getUserRole(Long userId);

    List<MenuVo> buildMenus(String role);
}
