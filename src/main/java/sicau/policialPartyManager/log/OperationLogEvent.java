package sicau.policialPartyManager.log;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 操作日志事件：业务点只负责发布，写 ES 由监听器异步完成（解耦）。
 */
@Getter
public class OperationLogEvent extends ApplicationEvent {

    private final OperationLog log;

    public OperationLogEvent(Object source, OperationLog log) {
        super(source);
        this.log = log;
    }
}
