package sicau.policialPartyManager.api.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "历史记录", description = "历史记录相关接口")
@RestController
@RequestMapping("/api/v4/history")
@PreAuthorize("hasRole('ROLE_ADMIN')")
@RequiredArgsConstructor
public class HistoryController {
}
