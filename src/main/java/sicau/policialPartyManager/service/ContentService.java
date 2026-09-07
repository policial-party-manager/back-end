package sicau.policialPartyManager.service;

import sicau.policialPartyManager.api.dto.ActivityVo;
import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.model.entity.ActivityType;
import sicau.policialPartyManager.model.entity.News;
import sicau.policialPartyManager.model.entity.Notice;

import java.util.List;

/**
 * 内容只读服务（新闻/公告/活动）：登录即可访问，仅返回“已发布/在展示期”的公开内容。
 */
public interface ContentService {

    /** 已上线新闻分页（keyword 匹配标题/正文，可按类型过滤） */
    PageResult<News> pageNews(long page, long size, String keyword, String type);

    News getNews(Long id);

    /** 在展示期内的公告分页（未删除且 startTime ≤ now ≤ endTime（endTime 可空）） */
    PageResult<Notice> pageNotices(long page, long size, String keyword);

    Notice getNotice(Long id);

    /** 已发布活动分页（活动状态=1；可按支部/类型过滤） */
    PageResult<ActivityVo> pageActivities(long page, long size, String keyword, Long branchId, Integer type);

    /** 活动详情（仅已发布） */
    ActivityVo getActivity(Long id);

    /** 活动类型列表 */
    List<ActivityType> listActivityTypes();
}
