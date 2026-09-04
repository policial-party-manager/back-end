package sicau.policialPartyManager.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.model.entity.Notice;
import sicau.policialPartyManager.repository.NoticeMapper;
import sicau.policialPartyManager.service.AdminNoticeService;

import java.time.LocalDateTime;

/**
 * 公告管理（后台）：分页搜索、增删改查、发布/标记删除。
 */
@Service
@RequiredArgsConstructor
public class AdminNoticeServiceImpl implements AdminNoticeService {

    private final NoticeMapper noticeMapper;

    @Override
    public PageResult<Notice> pageNotices(long page, long size, String keyword, Integer status) {
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();
        // 默认不展示“已删除(status=2)”，显式传 status=2 时才展示
        if (status == null) {
            wrapper.ne(Notice::getStatus, 2);
        } else {
            wrapper.eq(Notice::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            String k = keyword.trim();
            wrapper.and(w -> w.like(Notice::getTitle, k).or().like(Notice::getContent, k));
        }
        wrapper.orderByDesc(Notice::getPublishTime).orderByDesc(Notice::getId);
        Page<Notice> result = noticeMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public Notice getNotice(Long id) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new IllegalArgumentException("公告不存在");
        }
        return notice;
    }

    @Override
    public void createNotice(Notice notice, Long operatorId) {
        if (!StringUtils.hasText(notice.getTitle())) {
            throw new IllegalArgumentException("公告标题不能为空");
        }
        notice.setId(null);
        notice.setPublisherId(operatorId);
        if (notice.getStatus() == null) {
            notice.setStatus(1);
        }
        if (notice.getPublishTime() == null) {
            notice.setPublishTime(LocalDateTime.now());
        }
        noticeMapper.insert(notice);
    }

    @Override
    public void updateNotice(Long id, Notice notice, Long operatorId) {
        Notice existing = noticeMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("公告不存在");
        }
        if (StringUtils.hasText(notice.getTitle())) {
            existing.setTitle(notice.getTitle());
        }
        existing.setContent(notice.getContent());
        existing.setStartTime(notice.getStartTime());
        existing.setEndTime(notice.getEndTime());
        if (notice.getStatus() != null) {
            existing.setStatus(notice.getStatus());
        }
        noticeMapper.updateById(existing);
    }

    @Override
    public void deleteNotice(Long id) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new IllegalArgumentException("公告不存在");
        }
        notice.setStatus(2);
        noticeMapper.updateById(notice);
    }

    @Override
    public void publishNotice(Long id) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new IllegalArgumentException("公告不存在");
        }
        notice.setStatus(1);
        notice.setPublishTime(LocalDateTime.now());
        noticeMapper.updateById(notice);
    }
}
