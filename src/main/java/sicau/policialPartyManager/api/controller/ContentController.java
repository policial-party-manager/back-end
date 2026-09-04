package sicau.policialPartyManager.api.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "内容", description = "新闻,活动,通知获取的相关功能")
@RestController
@RequestMapping("/api/v2/admin")
@RequiredArgsConstructor
public class ContentController {
}
