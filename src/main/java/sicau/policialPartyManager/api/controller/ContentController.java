package sicau.policialPartyManager.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.api.dto.Result;
import sicau.policialPartyManager.model.entity.Activity;
import sicau.policialPartyManager.model.entity.News;
import sicau.policialPartyManager.model.entity.Notice;
import sicau.policialPartyManager.repository.ActivityMapper;
import sicau.policialPartyManager.repository.NewsMapper;
import sicau.policialPartyManager.repository.NoticeMapper;
import sicau.policialPartyManager.api.dto.ActivityVo;
import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.api.dto.Result;
import sicau.policialPartyManager.model.entity.ActivityType;
import sicau.policialPartyManager.model.entity.News;
import sicau.policialPartyManager.model.entity.Notice;
import sicau.policialPartyManager.service.ContentService;

import java.util.List;

/**
 * 内容公开接口（普通成员可访问）
 * 新闻、活动、公告的公开查询
 *
 * 内容服务：新闻/公告/活动的查询（列表与详情）。登录即可访问，权限宽泛；
 * 只返回“已发布/在展示期”的公开内容，管理后台的写操作仍在 AdminController。
 */
@Tag(name = "内容", description = "新闻、公告、活动的公开查询（登录即可）")
@RestController
@RequestMapping("/api/v2/content")
@RequiredArgsConstructor
public class ContentController {

    private final NewsMapper newsMapper;
    private final ActivityMapper activityMapper;
    private final NoticeMapper noticeMapper;

    // ==================== 新闻 ====================

    @Operation(summary = "新闻分页列表", description = "keyword 匹配标题，可按类型/状态过滤；status=1 表示已发布")
    @GetMapping("/news/page")
    public Result<PageResult<News>> pageNews(@RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) String type,
                                              @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<News> q = new LambdaQueryWrapper<>();
        q.eq(status != null, News::getStatus, status)
         .eq(type != null, News::getType, type)
         .like(keyword != null, News::getTitle, keyword)
         .orderByDesc(News::getCreateTime);
        long total = newsMapper.selectCount(q);
        q.last("LIMIT " + (page - 1) * size + ", " + size);
        List<News> records = newsMapper.selectList(q);
        return Result.ok(PageResult.of(records, total, page, size));
    }

    @Operation(summary = "新闻详情")
    @GetMapping("/news/{id}")
    public Result<News> getNews(@PathVariable Long id) {
        News news = newsMapper.selectById(id);
        if (news != null && news.getViewCount() != null) {
            news.setViewCount(news.getViewCount() + 1);
            newsMapper.updateById(news);
        }
        return Result.ok(news);
    }

    @Operation(summary = "最新新闻列表", description = "返回已发布的新闻，按创建时间倒序")
    @GetMapping("/news/latest")
    public Result<List<News>> latestNews(@RequestParam(defaultValue = "5") int limit) {
        LambdaQueryWrapper<News> q = new LambdaQueryWrapper<>();
        q.eq(News::getStatus, 1)
         .orderByDesc(News::getCreateTime)
         .last("LIMIT " + limit);
        return Result.ok(newsMapper.selectList(q));
    }

    // ==================== 活动 ====================

    @Operation(summary = "活动分页列表", description = "keyword 匹配标题；branchId 过滤支部")
    @GetMapping("/activity/page")
    public Result<PageResult<Activity>> pageActivity(@RequestParam(defaultValue = "1") long page,
                                                      @RequestParam(defaultValue = "10") long size,
                                                      @RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) Long branchId) {
        LambdaQueryWrapper<Activity> q = new LambdaQueryWrapper<>();
        q.like(keyword != null, Activity::getTitle, keyword)
         .eq(branchId != null, Activity::getBranchId, branchId)
         .orderByDesc(Activity::getCreateTime);
        long total = activityMapper.selectCount(q);
        q.last("LIMIT " + (page - 1) * size + ", " + size);
        List<Activity> records = activityMapper.selectList(q);
        return Result.ok(PageResult.of(records, total, page, size));
    }

    @Operation(summary = "活动详情")
    @GetMapping("/activity/{id}")
    public Result<Activity> getActivity(@PathVariable Long id) {
        return Result.ok(activityMapper.selectById(id));
    }

    @Operation(summary = "可参与的活动列表", description = "返回最近的活动")
    @GetMapping("/activity/available")
    public Result<List<Activity>> availableActivities() {
        LambdaQueryWrapper<Activity> q = new LambdaQueryWrapper<>();
        q.orderByDesc(Activity::getCreateTime)
         .last("LIMIT 20");
        return Result.ok(activityMapper.selectList(q));
    }

    // ==================== 公告 ====================

    @Operation(summary = "公告分页列表", description = "keyword 匹配标题；status=1 表示已发布")
    @GetMapping("/notice/page")
    public Result<PageResult<Notice>> pageNotice(@RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "10") long size,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<Notice> q = new LambdaQueryWrapper<>();
        q.eq(status != null, Notice::getStatus, status)
         .like(keyword != null, Notice::getTitle, keyword)
         .orderByDesc(Notice::getPublishTime);
        long total = noticeMapper.selectCount(q);
        q.last("LIMIT " + (page - 1) * size + ", " + size);
        List<Notice> records = noticeMapper.selectList(q);
        return Result.ok(PageResult.of(records, total, page, size));
    }

    @Operation(summary = "公告详情")
    @GetMapping("/notice/{id}")
    public Result<Notice> getNotice(@PathVariable Long id) {
        return Result.ok(noticeMapper.selectById(id));
    }

    @Operation(summary = "最新公告列表", description = "返回已发布的公告，按发布时间倒序")
    @GetMapping("/notice/latest")
    public Result<List<Notice>> latestNotices(@RequestParam(defaultValue = "5") int limit) {
        LambdaQueryWrapper<Notice> q = new LambdaQueryWrapper<>();
        q.eq(Notice::getStatus, 1)
         .orderByDesc(Notice::getPublishTime)
         .last("LIMIT " + limit);
        return Result.ok(noticeMapper.selectList(q));
    }
}
