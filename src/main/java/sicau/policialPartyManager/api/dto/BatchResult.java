package sicau.policialPartyManager.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量操作结果（#82）。
 * <p>沿用 Excel 导入已定型的「部分成功」语义：成功的条目已生效，
 * 失败的条目在明细中说明原因，不整体回滚。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchResult {

    /** 提交总数 */
    private int total;
    /** 成功数 */
    private int successCount;
    /** 失败数 */
    private int failCount;
    /** 逐条失败明细 */
    private List<Item> errors = new ArrayList<>();

    public static BatchResult of(int total) {
        BatchResult r = new BatchResult();
        r.total = total;
        return r;
    }

    public void success() {
        this.successCount++;
    }

    public void fail(Long id, String reason) {
        this.failCount++;
        this.errors.add(new Item(id, reason));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private Long id;
        private String reason;
    }
}
