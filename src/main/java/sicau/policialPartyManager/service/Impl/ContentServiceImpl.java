package sicau.policialPartyManager.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import sicau.policialPartyManager.api.dto.ActivityVo;
import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.model.entity.Activity;
import sicau.policialPartyManager.model.entity.ActivityDetail;
import sicau.policialPartyManager.model.entity.ActivityType;
import sicau.policialPartyManager.model.entity.Branch;
import sicau.policialPartyManager.model.entity.News;
import sicau.policialPartyManager.model.entity.Notice;
import sicau.policialPartyManager.repository.ActivityDetailMapper;
import sicau.policialPartyManager.repository.ActivityMapper;
import sicau.policialPartyManager.repository.ActivityTypeMapper;
import sicau.policialPartyManager.repository.BranchMapper;
import sicau.policialPartyManager.repository.NewsMapper;
import sicau.policialPartyManager.repository.NoticeMapper;
import sicau.policialPartyManager.service.ContentService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 内容只读服务：新闻（仅上线）、公告（展示期内）、活动（仅已发布）的列表与详情。
 */
@Service
@RequiredArgsConstructor
public class ContentServiceImpl implements ContentService {

    private final NewsMapper newsMapper;
    private final NoticeMapper noticeMapper;
    private final ActivityMapper activityMapper;
    private final ActivityDetailMapper activityDetailMapper;
    private final ActivityTypeMapper activityTypeMapper;
    private final BranchMapper branchMapper;

    @Override
    public PageResult<News> pageNews(long page, long size, String keyword, String type) {
        LambdaQueryWrapper<News> wrapper = new LambdaQueryWrapper<News>().eq(News::getStatus, 1);
        if (StringUtils.hasText(keyword)) {
            String k = keyword.trim();
            wrapper.and(w -> w.like(News::getTitle, k).or().like(News::getContent, k));
        }
        if (StringUtils.hasText(type)) {
            wrapper.eq(News::getType, type.trim());
        }
        wrapper.orderByDesc(News::getCreateTime).orderByDesc(News::getId);
        Page<News> result = newsMapper.selectPage(new Page<>(safePage(page), safeSize(size)), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), safePage(page), safeSize(size));
    }

    @Override
    public News getNews(Long id) {
        News news = newsMapper.selectById(id);
        if (news == null || news.getStatus() == null || news.getStatus() != 1) {
            throw new IllegalArgumentException("新闻不存在或已下线");
        }
        return news;
    }

    @Override
    public PageResult<Notice> pageNotices(long page, long size, String keyword) {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<Notice> wrapper = visibleNoticeWrapper(now);
        if (StringUtils.hasText(keyword)) {
            String k = keyword.trim();
            wrapper.and(w -> w.like(Notice::getTitle, k).or().like(Notice::getContent, k));
        }
        wrapper.orderByDesc(Notice::getPublishTime).orderByDesc(Notice::getId);
        Page<Notice> result = noticeMapper.selectPage(new Page<>(safePage(page), safeSize(size)), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), safePage(page), safeSize(size));
    }

    @Override
    public Notice getNotice(Long id) {
        Notice notice = noticeMapper.selectOne(visibleNoticeWrapper(LocalDateTime.now())
                .eq(Notice::getId, id));
        if (notice == null) {
            throw new IllegalArgumentException("公告不存在或不在展示期");
        }
        return notice;
    }

    @Override
    public PageResult<ActivityVo> pageActivities(long page, long size, String keyword, Long branchId, Integer type) {
        long safePage = safePage(page);
        long safeSize = safeSize(size);

        // 仅展示已发布（detail.status=1）的活动
        Set<Long> publishedIds = activityDetailMapper.selectList(new LambdaQueryWrapper<ActivityDetail>()
                        .eq(ActivityDetail::getStatus, 1)).stream()
                .map(ActivityDetail::getActivityId).collect(Collectors.toSet());
        if (publishedIds.isEmpty()) {
            return PageResult.of(Collections.emptyList(), 0, safePage, safeSize);
        }

        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<Activity>()
                .in(Activity::getId, publishedIds);
        if (branchId != null) {
            wrapper.eq(Activity::getBranchId, branchId);
        }
        if (type != null) {
            wrapper.eq(Activity::getType, type);
        }
        if (StringUtils.hasText(keyword)) {
            String k = keyword.trim();
            wrapper.and(w -> w.like(Activity::getTitle, k).or().like(Activity::getDescription, k));
        }
        wrapper.orderByDesc(Activity::getStartTime).orderByDesc(Activity::getId);
        Page<Activity> result = activityMapper.selectPage(new Page<>(safePage, safeSize), wrapper);
        List<ActivityVo> vos = buildVos(result.getRecords());
        return PageResult.of(vos, result.getTotal(), safePage, safeSize);
    }

    @Override
    public ActivityVo getActivity(Long id) {
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new IllegalArgumentException("活动不存在");
        }
        ActivityVo vo = buildVos(List.of(activity)).get(0);
        if (vo.getStatus() == null || vo.getStatus() != 1) {
            throw new IllegalArgumentException("活动不存在或已下架");
        }
        return vo;
    }

    @Override
    public List<ActivityType> listActivityTypes() {
        return activityTypeMapper.selectList(new LambdaQueryWrapper<ActivityType>().orderByAsc(ActivityType::getId));
    }

    // ======================= 私有辅助 =======================

    /** 公告可见条件：未删除(status≠2)，且开始时间未到则不可见、结束时间已过则不可见（时间可空=不限） */
    private LambdaQueryWrapper<Notice> visibleNoticeWrapper(LocalDateTime now) {
        return new LambdaQueryWrapper<Notice>()
                .ne(Notice::getStatus, 2)
                .and(w -> w.isNull(Notice::getStartTime).or().le(Notice::getStartTime, now))
                .and(w -> w.isNull(Notice::getEndTime).or().ge(Notice::getEndTime, now));
    }

    private List<ActivityVo> buildVos(List<Activity> activities) {
        if (activities.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> ids = activities.stream().map(Activity::getId).collect(Collectors.toList());

        Map<Long, ActivityDetail> detailMap = activityDetailMapper.selectList(
                        new LambdaQueryWrapper<ActivityDetail>().in(ActivityDetail::getActivityId, ids)).stream()
                .collect(Collectors.toMap(ActivityDetail::getActivityId, Function.identity(), (a, b) -> a));

        Set<Long> branchIds = activities.stream().map(Activity::getBranchId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Branch> branchMap = branchIds.isEmpty() ? Collections.emptyMap()
                : branchMapper.selectBatchIds(branchIds).stream()
                .collect(Collectors.toMap(Branch::getId, Function.identity(), (a, b) -> a));

        Set<Integer> typeIds = activities.stream().map(Activity::getType)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> typeLongIds = typeIds.stream().map(Integer::longValue).collect(Collectors.toSet());
        Map<Long, ActivityType> typeMap = typeLongIds.isEmpty() ? Collections.emptyMap()
                : activityTypeMapper.selectBatchIds(typeLongIds).stream()
                .collect(Collectors.toMap(ActivityType::getId, Function.identity(), (a, b) -> a));

        List<ActivityVo> vos = new ArrayList<>();
        for (Activity activity : activities) {
            ActivityVo vo = new ActivityVo();
            vo.setId(activity.getId());
            vo.setBranchId(activity.getBranchId());
            vo.setTitle(activity.getTitle());
            vo.setDescription(activity.getDescription());
            vo.setType(activity.getType());
            vo.setCover(activity.getCover());
            vo.setStartTime(activity.getStartTime());
            vo.setEndTime(activity.getEndTime());
            vo.setCreateTime(activity.getCreateTime());
            vo.setUpdateTime(activity.getUpdateTime());

            Branch branch = activity.getBranchId() == null ? null : branchMap.get(activity.getBranchId());
            vo.setBranchName(branch == null ? null : branch.getBranchName());
            ActivityType type = activity.getType() == null ? null : typeMap.get(activity.getType().longValue());
            vo.setTypeName(type == null ? null : type.getName());

            ActivityDetail detail = detailMap.get(activity.getId());
            if (detail != null) {
                vo.setLocation(detail.getLocation());
                vo.setLocationDescription(detail.getLocationDescription());
                vo.setSignStart(detail.getSignStart());
                vo.setSignEnd(detail.getSignEnd());
                vo.setMaxParticipants(detail.getMaxParticipants());
                vo.setStatus(detail.getStatus());
                vo.setCreatorId(detail.getCreatorId());
            }
            vos.add(vo);
        }
        return vos;
    }

    private long safePage(long page) {
        return Math.max(1, page);
    }

    private long safeSize(long size) {
        return Math.min(Math.max(1, size), 100);
    }
}
