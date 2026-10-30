package sicau.policialPartyManager.service;

import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.api.dto.StudySaveRequest;
import sicau.policialPartyManager.model.entity.Study;

/**
 * 课程字典服务（#32，超管维护）。
 * <p>
 * 名称全表唯一（应用层校验）；type 为自由文本、不预设枚举；
 * 有学习记录引用的课程不可删除。
 */
public interface StudyService {

    /** 新增课程；重名 400 */
    Study create(StudySaveRequest request);

    /** 编辑课程；重名（不含自身）400；不存在 400 */
    Study update(Long id, StudySaveRequest request);

    /** 删除课程；被学习记录引用时拒绝 */
    void delete(Long id);

    /** 课程分页（登录用户均可读，供录入选课）；keyword 匹配名称，type 精确过滤 */
    PageResult<Study> page(long page, long size, String keyword, String type);
}
