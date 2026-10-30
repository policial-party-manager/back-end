package sicau.policialPartyManager.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import sicau.policialPartyManager.service.Impl.AdminBranchServiceImpl;
import sicau.policialPartyManager.service.Impl.AdminUserServiceImpl;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 导入模板的文档契约测试。
 * <p>
 * 背景：#43 里 {@code downloadBranchTemplate} 的 Swagger 描述还写着 3 列，
 * 而实现早已是 4 列（PR #29 加了「书记用户id」）；用户模板同样只写了前 6 列，
 * 实际有 13 列。这类漂移编译期发现不了，用户照文档填就会导入失败。
 * <p>
 * 做法：直接让服务生成真实的 .xlsx，读出表头，再逐列断言 Swagger 描述里出现过该列名。
 * 不需要 Spring 上下文，也不需要数据库——{@code template()} 只依赖 ExcelUtil。
 */
class ImportTemplateDocContractTest {

    @Test
    void branchTemplateDocListsEveryColumn() throws Exception {
        byte[] xlsx = new AdminBranchServiceImpl(null).template();
        assertDocCoversTemplate(
                AdminController.class.getMethod("downloadBranchTemplate", HttpServletResponse.class),
                xlsx);
    }

    @Test
    void userTemplateDocListsEveryColumn() throws Exception {
        byte[] xlsx = new AdminUserServiceImpl(null, null, null, null, null, null, null).template();
        assertDocCoversTemplate(
                AdminController.class.getMethod("downloadUserTemplate", HttpServletResponse.class),
                xlsx);
    }

    private void assertDocCoversTemplate(Method endpoint, byte[] xlsx) throws Exception {
        Operation operation = endpoint.getAnnotation(Operation.class);
        assertTrue(operation != null, endpoint.getName() + " 缺少 @Operation");
        String description = operation.description();

        List<String> headers = readHeaderRow(xlsx);
        assertTrue(!headers.isEmpty(), endpoint.getName() + " 生成的模板没有表头");

        List<String> missing = new ArrayList<>();
        for (String header : headers) {
            if (!description.contains(header)) {
                missing.add(header);
            }
        }
        assertTrue(missing.isEmpty(), () ->
                endpoint.getName() + " 的 Swagger 描述漏了模板列 " + missing
                        + "\n当前描述：" + description
                        + "\n模板实际表头：" + headers);
    }

    /** 读出模板第一行（表头），忽略空单元格 */
    private List<String> readHeaderRow(byte[] xlsx) throws Exception {
        List<String> headers = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(xlsx))) {
            Row header = workbook.getSheetAt(0).getRow(0);
            for (int i = 0; i < header.getLastCellNum(); i++) {
                String text = header.getCell(i) == null ? "" : header.getCell(i).getStringCellValue();
                if (!text.isBlank()) {
                    headers.add(text.trim());
                }
            }
        }
        return headers;
    }
}
