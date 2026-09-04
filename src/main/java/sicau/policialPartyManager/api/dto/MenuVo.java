package sicau.policialPartyManager.api.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class MenuVo {
    private String name;
    private String path;
    private String icon;
    private List<MenuVo> children;
}
