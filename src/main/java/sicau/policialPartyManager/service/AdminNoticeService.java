package sicau.policialPartyManager.service;

import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.model.entity.Notice;

public interface AdminNoticeService {

    /** 分页搜索公告：keyword 匹配标题/内容；可按状态过滤 */
    PageResult<Notice> pageNotices(long page, long size, String keyword, Integer status);

    Notice getNotice(Long id);

    /** 新增公告（publisherId=当前操作者；默认发布 status=1，publishTime=now） */
    void createNotice(Notice notice, Long operatorId);

    /** 编辑公告 */
    void updateNotice(Long id, Notice notice, Long operatorId);

    /** 删除公告（标记删除 status=2，保留记录） */
    void deleteNotice(Long id);

    /** 发布（status=1，publishTime=now） */
    void publishNotice(Long id);
}
