package sicau.policialPartyManager.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Excel 批量导入结果：成功/失败计数 + 逐行失败原因（便于用户修正后重新上传）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportResult {

    /** 数据总行数（不含表头） */
    private int total;

    /** 成功导入行数 */
    private int successCount;

    /** 失败行数 */
    private int failCount;

    /** 失败明细（Excel 实际行号 + 原因） */
    private List<RowError> errors = new ArrayList<>();

    public static ImportResult of(int total, int successCount, List<RowError> errors) {
        return new ImportResult(total, successCount, total - successCount, errors);
    }

    /** 单行导入失败信息 */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RowError {
        /** Excel 实际行号（表头为第 1 行，数据从第 2 行开始） */
        private int row;
        /** 失败原因 */
        private String reason;
    }
}
