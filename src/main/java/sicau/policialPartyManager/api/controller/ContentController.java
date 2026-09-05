package sicau.policialPartyManager.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import sicau.policialPartyManager.api.dto.ActivityVo;
import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.api.dto.Result;
import sicau.policialPartyManager.model.entity.ActivityType;
import sicau.policialPartyManager.model.entity.News;
import sicau.policialPartyManager.model.entity.Notice;
import sicau.policialPartyManager.service.ContentService;

import java.util.List;

/**
 * 内容服务：新闻/公告/活动的查询（列表与详情）。登录即可访问，权限宽泛；
 * 只返回“已发布/在展示期”的公开内容，管理后台的写操作仍在 AdminController。
 */
@Tag(name = "内容", description = "新闻、公告、活动的公开查询（登录即可）")
@RestController
@RequestMapping("/api/v2/content")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    @Operation(summary = "新闻列表", description = "仅已上线新闻；keyword 匹配标题/正文，可按类型过滤")
    @GetMapping("/news/page")
    public Result<PageResult<News>> pageNews(@RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "10") long size,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) String type) {
        return Result.ok(contentService.pageNews(page, size, keyword, type));
    }

    @Operation(summary = "新闻详情")
    @GetMapping("/news/{id}")
    public Result<News> getNews(@PathVariable Long id) {
        return Result.ok(contentService.getNews(id));
    }

    @Operation(summary = "公告列表", description = "展示期内（startTime ≤ now ≤ endTime，可空不限）且未删除的公告")
    @GetMapping("/notices/page")
    public Result<PageResult<Notice>> pageNotices(@RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "10") long size,
                                                  @RequestParam(required = false) String keyword) {
        return Result.ok(contentService.pageNotices(page, size, keyword));
    }

    @Operation(summary = "公告详情")
    @GetMapping("/notices/{id}")
    public Result<Notice> getNotice(@PathVariable Long id) {
        return Result.ok(contentService.getNotice(id));
    }

    @Operation(summary = "活动列表", description = "仅已发布活动（status=1）；可按支部/类型过滤")
    @GetMapping("/activities/page")
    public Result<PageResult<ActivityVo>> pageActivities(@RequestParam(defaultValue = "1") long page,
                                                         @RequestParam(defaultValue = "10") long size,
                                                         @RequestParam(required = false) String keyword,
                                                         @RequestParam(required = false) Long branchId,
                                                         @RequestParam(required = false) Integer type) {
        return Result.ok(contentService.pageActivities(page, size, keyword, branchId, type));
    }

    @Operation(summary = "活动详情", description = "含封面/简介/地点/人数上限等（仅已发布）")
    @GetMapping("/activities/{id}")
    public Result<ActivityVo> getActivity(@PathVariable Long id) {
        return Result.ok(contentService.getActivity(id));
    }

    @Operation(summary = "活动类型列表")
    @GetMapping("/activity-types")
    public Result<List<ActivityType>> listActivityTypes() {
        return Result.ok(contentService.listActivityTypes());
    }
}
