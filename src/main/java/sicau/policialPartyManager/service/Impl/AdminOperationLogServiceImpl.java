package sicau.policialPartyManager.service.Impl;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import sicau.policialPartyManager.api.dto.PageResult;
import sicau.policialPartyManager.log.LogType;
import sicau.policialPartyManager.log.OperationLog;
import sicau.policialPartyManager.service.AdminOperationLogService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一操作日志查询实现（ES）。ES 不可用/索引未就绪时抛出 IllegalStateException，
 * 由全局异常处理器提示“日志服务暂不可用”。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOperationLogServiceImpl implements AdminOperationLogService {

    private static final DateTimeFormatter LOCAL_DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<String> LOG_TYPES = List.of(LogType.LOGIN, LogType.LOGOUT,
            LogType.ERROR, LogType.OPERATION);

    private final ElasticsearchOperations esOperations;

    @Override
    public PageResult<OperationLog> pageLogs(long page, long size, String logType, String module,
                                             String operatorName, String ip, String keyword,
                                             Boolean success, String errorType, String startTime, String endTime) {
        long safePage = Math.max(1, page);
        long safeSize = Math.min(Math.max(1, size), 200);
        Long fromMs = parseTime(startTime);
        Long toMs = parseTime(endTime);

        Query query = buildQuery(logType, module, operatorName, ip, keyword, success, errorType, fromMs, toMs);
        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(query)
                .withPageable(PageRequest.of((int) safePage - 1, (int) safeSize))
                .withSort(Sort.by(Sort.Order.desc("occurTime")))
                .build();
        SearchHits<OperationLog> hits = search(nativeQuery);
        List<OperationLog> records = hits.getSearchHits().stream()
                .map(hit -> hit.getContent()).toList();
        return PageResult.of(records, hits.getTotalHits(), safePage, safeSize);
    }

    @Override
    public Map<String, Long> stats() {
        Map<String, Long> result = new LinkedHashMap<>();
        for (String type : LOG_TYPES) {
            result.put(type, count(buildQuery(type, null, null, null, null, null, null, null, null)));
        }
        return result;
    }

    @Override
    public long cleanup(int days) {
        if (days < 1 || days > 730) {
            throw new IllegalArgumentException("保留天数须在 1~730 之间");
        }
        long cutoff = System.currentTimeMillis() - days * 86_400_000L;
        Query query = Query.of(q -> q.bool(b -> b.must(
                Query.of(x -> x.range(r -> r.field("occurTime").lt(JsonData.of(cutoff)))))));
        long count = count(query);
        NativeQuery deleteQuery = NativeQuery.builder().withQuery(query).build();
        try {
            esOperations.delete(deleteQuery, OperationLog.class);
            return count;
        } catch (Exception e) {
            throw new IllegalStateException("Elasticsearch 日志清理失败：" + rootMessage(e));
        }
    }

    // ======================= 私有辅助 =======================

    private Query buildQuery(String logType, String module, String operatorName, String ip,
                             String keyword, Boolean success, String errorType,
                             Long fromMs, Long toMs) {
        List<Query> musts = new ArrayList<>();
        if (StringUtils.hasText(logType)) {
            musts.add(term("logType", logType.trim()));
        }
        if (StringUtils.hasText(module)) {
            musts.add(term("module", module.trim()));
        }
        if (StringUtils.hasText(ip)) {
            musts.add(term("ipAddress", ip.trim()));
        }
        if (StringUtils.hasText(errorType)) {
            musts.add(term("errorType", errorType.trim()));
        }
        if (success != null) {
            musts.add(Query.of(x -> x.term(t -> t.field("success").value(FieldValue.of(success)))));
        }
        if (StringUtils.hasText(operatorName)) {
            String name = operatorName.trim();
            musts.add(Query.of(x -> x.wildcard(w -> w.field("operatorName.keyword").value("*" + name + "*"))));
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            musts.add(Query.of(x -> x.queryString(qs -> qs
                    .fields(List.of("action", "message", "path", "operatorName"))
                    .query(kw))));
        }
        if (fromMs != null || toMs != null) {
            musts.add(Query.of(x -> x.range(r -> {
                r.field("occurTime");
                if (fromMs != null) {
                    r.gte(JsonData.of(fromMs));
                }
                if (toMs != null) {
                    r.lte(JsonData.of(toMs));
                }
                return r;
            })));
        }

        if (musts.isEmpty()) {
            return Query.of(q -> q.matchAll(m -> m));
        }
        if (musts.size() == 1) {
            return musts.get(0);
        }
        return Query.of(q -> q.bool(b -> {
            for (Query mq : musts) {
                b.must(mq);
            }
            return b;
        }));
    }

    private Query term(String field, String value) {
        return Query.of(q -> q.term(t -> t.field(field).value(FieldValue.of(value))));
    }

    private SearchHits<OperationLog> search(NativeQuery query) {
        try {
            return esOperations.search(query, OperationLog.class);
        } catch (Exception e) {
            throw new IllegalStateException("Elasticsearch 日志服务暂不可用：" + rootMessage(e));
        }
    }

    private long count(Query query) {
        try {
            NativeQuery nativeQuery = NativeQuery.builder().withQuery(query)
                    .withPageable(PageRequest.of(0, 1)).build();
            SearchHits<OperationLog> hits = esOperations.search(nativeQuery, OperationLog.class);
            return hits.getTotalHits();
        } catch (Exception e) {
            throw new IllegalStateException("Elasticsearch 日志服务暂不可用：" + rootMessage(e));
        }
    }

    /** 解析时间入参：ISO-8601 或 yyyy-MM-dd HH:mm:ss（本机时区） */
    private Long parseTime(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String value = text.trim();
        try {
            return Instant.parse(value).toEpochMilli();
        } catch (Exception ignored) {
            // 尝试本地时间格式
        }
        try {
            LocalDateTime local = LocalDateTime.parse(value, LOCAL_DT);
            return local.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception e) {
            throw new IllegalArgumentException("时间格式不正确，请使用 ISO-8601 或 yyyy-MM-dd HH:mm:ss");
        }
    }

    private String rootMessage(Exception e) {
        Throwable cur = e;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        String msg = cur.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = cur.getClass().getSimpleName();
        }
        return msg.length() > 200 ? msg.substring(0, 200) : msg;
    }
}
