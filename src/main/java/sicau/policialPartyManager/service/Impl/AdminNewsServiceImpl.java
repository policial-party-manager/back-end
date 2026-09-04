package sicau.policialPartyManager.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.model.entity.News;
import sicau.policialPartyManager.repository.NewsMapper;
import sicau.policialPartyManager.service.AdminNewsService;

/**
 * 新闻管理（后台）：分页搜索、增删改查、发布/下线。
 */
@Service
@RequiredArgsConstructor
public class AdminNewsServiceImpl implements AdminNewsService {

    private final NewsMapper newsMapper;

    @Override
    public PageResult<News> pageNews(long page, long size, String keyword, String type, Integer status) {
        LambdaQueryWrapper<News> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String k = keyword.trim();
            wrapper.and(w -> w.like(News::getTitle, k).or().like(News::getContent, k));
        }
        if (StringUtils.hasText(type)) {
            wrapper.eq(News::getType, type.trim());
        }
        if (status != null) {
            wrapper.eq(News::getStatus, status);
        }
        wrapper.orderByDesc(News::getCreateTime).orderByDesc(News::getId);
        Page<News> result = newsMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public News getNews(Long id) {
        News news = newsMapper.selectById(id);
        if (news == null) {
            throw new IllegalArgumentException("新闻不存在");
        }
        return news;
    }

    @Override
    public void createNews(News news, Long operatorId) {
        if (!StringUtils.hasText(news.getTitle())) {
            throw new IllegalArgumentException("新闻标题不能为空");
        }
        news.setId(null);
        news.setAuthor(operatorId);
        if (news.getStatus() == null) {
            news.setStatus(1);
        }
        if (news.getViewCount() == null) {
            news.setViewCount(0);
        }
        newsMapper.insert(news);
    }

    @Override
    public void updateNews(Long id, News news, Long operatorId) {
        News existing = newsMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("新闻不存在");
        }
        if (StringUtils.hasText(news.getTitle())) {
            existing.setTitle(news.getTitle());
        }
        existing.setCover(news.getCover());
        existing.setType(news.getType());
        existing.setContent(news.getContent());
        if (news.getStatus() != null) {
            existing.setStatus(news.getStatus());
        }
        newsMapper.updateById(existing);
    }

    @Override
    public void deleteNews(Long id) {
        if (newsMapper.selectById(id) == null) {
            throw new IllegalArgumentException("新闻不存在");
        }
        newsMapper.deleteById(id);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 1 && status != 2)) {
            throw new IllegalArgumentException("状态不合法（1 上线 / 2 下线）");
        }
        News news = newsMapper.selectById(id);
        if (news == null) {
            throw new IllegalArgumentException("新闻不存在");
        }
        news.setStatus(status);
        newsMapper.updateById(news);
    }
}
