package sicau.policialPartyManager.service;

import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.model.entity.News;

public interface AdminNewsService {

    /** 分页搜索新闻：keyword 匹配标题/内容；可按类型/状态过滤 */
    PageResult<News> pageNews(long page, long size, String keyword, String type, Integer status);

    News getNews(Long id);

    /** 新增新闻（author=当前操作者；status 默认 1） */
    void createNews(News news, Long operatorId);

    /** 编辑新闻 */
    void updateNews(Long id, News news, Long operatorId);

    /** 删除新闻（物理删除） */
    void deleteNews(Long id);

    /** 发布/下线（status） */
    void updateStatus(Long id, Integer status);
}
