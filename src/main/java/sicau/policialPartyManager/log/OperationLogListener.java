package sicau.policialPartyManager.log;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 操作日志监听器：异步消费 {@link OperationLogEvent} 写入 Elasticsearch。
 * <p>
 * ES 不可用/写入失败时只记录 warn，不影响业务主流程（降级到应用日志）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OperationLogListener {

    private final ElasticsearchOperations esOperations;

    /** 索引是否已就绪（进程内缓存，避免每次建索引） */
    private volatile boolean indexReady = false;

    @Async("operationLogExecutor")
    @EventListener
    public void onOperationLog(OperationLogEvent event) {
        OperationLog entry = event.getLog();
        if (entry == null) {
            return;
        }
        try {
            ensureIndex();
            IndexCoordinates coordinates = esOperations.indexOps(OperationLog.class).getIndexCoordinates();
            esOperations.save(entry, coordinates);
        } catch (Exception e) {
            log.warn("操作日志写入 Elasticsearch 失败（已降级到应用日志）：type={}, module={}, action={}",
                    entry.getLogType(), entry.getModule(), entry.getAction(), e);
        }
    }

    private void ensureIndex() {
        if (indexReady) {
            return;
        }
        IndexOperations indexOps = esOperations.indexOps(OperationLog.class);
        if (!indexOps.exists()) {
            indexOps.createWithMapping();
        }
        indexReady = true;
    }
}
