package sicau.policialPartyManager.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.api.dto.StudySaveRequest;
import sicau.policialPartyManager.model.entity.Study;
import sicau.policialPartyManager.model.entity.StudyRecord;
import sicau.policialPartyManager.repository.StudyMapper;
import sicau.policialPartyManager.repository.StudyRecordMapper;
import sicau.policialPartyManager.service.StudyService;

/**
 * 课程字典（#32，超管维护）。重名与「被引用不可删」均在应用层给出可读 400。
 */
@Service
@RequiredArgsConstructor
public class StudyServiceImpl implements StudyService {

    private final StudyMapper studyMapper;
    private final StudyRecordMapper studyRecordMapper;

    @Override
    public Study create(StudySaveRequest request) {
        String name = requireText(request.getName(), "课程名");
        checkNameFree(name, null);
        Study study = new Study();
        study.setName(name);
        study.setType(requireText(request.getType(), "课程类型"));
        studyMapper.insert(study);
        return study;
    }

    @Override
    public Study update(Long id, StudySaveRequest request) {
        Study existing = studyMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("课程不存在");
        }
        String name = requireText(request.getName(), "课程名");
        checkNameFree(name, id);
        existing.setName(name);
        existing.setType(requireText(request.getType(), "课程类型"));
        studyMapper.updateById(existing);
        return existing;
    }

    @Override
    public void delete(Long id) {
        Study existing = studyMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("课程不存在");
        }
        Long referencing = studyRecordMapper.selectCount(new LambdaQueryWrapper<StudyRecord>()
                .eq(StudyRecord::getCourseId, id));
        if (referencing != null && referencing > 0) {
            throw new IllegalArgumentException("该课程已有 " + referencing + " 条学习记录，不能删除");
        }
        studyMapper.deleteById(id);
    }

    @Override
    public PageResult<Study> page(long page, long size, String keyword, String type) {
        LambdaQueryWrapper<Study> wrapper = new LambdaQueryWrapper<Study>().orderByAsc(Study::getId);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Study::getName, keyword.trim());
        }
        if (StringUtils.hasText(type)) {
            wrapper.eq(Study::getType, type.trim());
        }
        Page<Study> result = studyMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    /** 同名（排除自身）视为冲突，对齐活动类型/支部的重名校验风格 */
    private void checkNameFree(String name, Long excludeId) {
        LambdaQueryWrapper<Study> wrapper = new LambdaQueryWrapper<Study>().eq(Study::getName, name);
        if (excludeId != null) {
            wrapper.ne(Study::getId, excludeId);
        }
        Long count = studyMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new IllegalArgumentException("课程名已存在");
        }
    }

    private String requireText(String value, String label) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(label + "不能为空");
        }
        return value.trim();
    }
}
