package sicau.policialPartyManager.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sicau.policialPartyManager.api.dto.DashboardVo;
import sicau.policialPartyManager.api.dto.DistributionVo;
import sicau.policialPartyManager.api.dto.Result;
import sicau.policialPartyManager.config.CurrentUser;
import sicau.policialPartyManager.model.records.User;
import sicau.policialPartyManager.service.Impl.DashboardServiceImpl;

/**
 * 首页统计（#81）：只做能驱动动作的数。
 * <p>超管看全院、支委只看本支部；纯展示类数字不做。
 */
@Tag(name = "首页统计", description = "待办数与阶段分布")
@RestController
@RequestMapping("/api/v4/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final DashboardServiceImpl dashboardService;

    @Operation(summary = "首页待办与阶段分布",
            description = "待审核思想汇报数、待审批身份变更数（支委限本支部），以及各政治身份人数分布")
    @GetMapping("/dashboard")
    public Result<DashboardVo> dashboard(@CurrentUser User operator) {
        return Result.ok(dashboardService.dashboard(operator));
    }

    @Operation(summary = "阶段分布（按维度切分）",
            description = "dimension 可选 college / major / className / grade，缺省 college")
    @GetMapping("/distribution")
    public Result<DistributionVo> distribution(@CurrentUser User operator,
                                              @RequestParam(required = false) String dimension) {
        return Result.ok(dashboardService.distribution(operator, dimension));
    }
}
