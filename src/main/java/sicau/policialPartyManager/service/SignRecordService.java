package sicau.policialPartyManager.service;

import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.api.dto.SignInRequest;
import sicau.policialPartyManager.model.entity.SignRecord;

/**
 * 活动签到服务（#35）。
 * <p>
 * 规则：仅已发布活动（detail.status=1）且在签到窗内可签；活动类型 need_sign=0 拒绝；
 * 同一用户同一活动只能签一次。错误统一 IllegalArgumentException（400）。
 */
public interface SignRecordService {

    /**
     * 成员签到。
     *
     * @return 落库后的签到记录（含 id 与 signTime）
     */
    SignRecord sign(SignInRequest request, Long userId);

    /**
     * 管理员：按活动分页查签到记录。
     *
     * @param userId 可选，按签到人过滤
     */
    PageResult<SignRecord> pageByActivity(long page, long size, Long activityId, Long userId);
}
