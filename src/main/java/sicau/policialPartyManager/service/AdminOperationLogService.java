package sicau.policialPartyManager.service;

import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.log.OperationLog;

import java.util.List;
import java.util.Map;

/**
 * 统一操作日志检索（Elasticsearch）
 */
public interface AdminOperationLogService {

    /**
     * 分页检索
     *
     * @param operatorName 操作者（模糊）
     * @param keyword      关键字（匹配 action/message/path/operatorName）
     * @param success      true/false/空
     * @param startTime    开始时间（ISO-8601 或 yyyy-MM-dd HH:mm:ss，可空）
     * @param endTime      结束时间（同上）
     */
    PageResult<OperationLog> pageLogs(long page, long size, String logType, String module,
                                      String operatorName, String ip, String keyword,
                                      Boolean success, String errorType, String startTime, String endTime);

    /** 按日志类型统计数量 */
    Map<String, Long> stats();

    /** 清理保留天数之前的日志，返回删除条数 */
    long cleanup(int days);
}
