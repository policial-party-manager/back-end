package sicau.policialPartyManager.service;

import sicau.policialPartyManager.api.dto.ActivitySaveRequest;
import sicau.policialPartyManager.api.dto.ActivityVo;
import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.model.entity.ActivityType;

import java.util.List;

public interface AdminActivityService {

    /** 分页搜索活动：keyword 匹配标题/简介；可按支部/类型/状态过滤 */
    PageResult<ActivityVo> pageActivities(long page, long size, String keyword, Long branchId, Integer type, Integer status);

    /** 活动详情（含详情信息与支部/类型名称） */
    ActivityVo getActivity(Long id);

    /** 新增活动（主体 + tb_activity_detail；creatorId=当前操作者；status 默认 1） */
    void createActivity(ActivitySaveRequest request, Long operatorId);

    /** 编辑活动（主体 + 详情） */
    void updateActivity(Long id, ActivitySaveRequest request, Long operatorId);

    /** 删除活动（连同详情记录） */
    void deleteActivity(Long id);

    /** 调整活动状态（0/1/2） */
    void updateActivityStatus(Long id, Integer status);

    // ===== 活动类型 =====

    /** 活动类型下拉/列表 */
    List<ActivityType> listActivityTypes();

    void saveActivityType(ActivityType type);

    void updateActivityType(Long id, ActivityType type);

    /** 删除活动类型（被活动引用时拒绝） */
    void deleteActivityType(Long id);
}
