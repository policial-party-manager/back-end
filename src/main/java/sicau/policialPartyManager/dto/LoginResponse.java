package sicau.policialPartyManager.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class LoginResponse {
    private String token;
    private Long userId;
    private String username;
    private String realName;
    private String role;
    private Long branchId;
    private String branchName;
    private List<MenuVo> menus;
}
