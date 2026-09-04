package sicau.policialPartyManager.log;

/**
 * 日志类型常量（对应 ES operation_log.logType，keyword 字段）
 */
public final class LogType {

    private LogType() {
    }

    /** 用户登录（含失败） */
    public static final String LOGIN = "LOGIN";

    /** 用户退出登录 */
    public static final String LOGOUT = "LOGOUT";

    /** 系统/业务错误 */
    public static final String ERROR = "ERROR";

    /** 业务操作（人员/管理员写操作等） */
    public static final String OPERATION = "OPERATION";
}
