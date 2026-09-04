package sicau.policialPartyManager.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel 工具：生成导入模板（.xlsx）与解析上传文件。
 * <p>
 * 约定：第 1 行为表头，数据从第 2 行开始；导入报告中的“行号”指 Excel 实际行号。
 */
public final class ExcelUtil {

    private ExcelUtil() {
    }

    /**
     * 生成带表头（可选示例行）的 .xlsx 模板
     *
     * @param sheetName 工作表名
     * @param headers   表头列名
     * @param examples  示例行（可为 null/空）
     * @return .xlsx 文件字节
     */
    public static byte[] createTemplate(String sheetName, String[] headers, String[][] examples) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 20 * 256);
            }

            if (examples != null) {
                int rowIdx = 1;
                for (String[] example : examples) {
                    if (example == null) {
                        continue;
                    }
                    Row row = sheet.createRow(rowIdx++);
                    for (int i = 0; i < example.length && i < headers.length; i++) {
                        if (example[i] != null) {
                            row.createCell(i).setCellValue(example[i]);
                        }
                    }
                }
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * 解析上传的 Excel，返回数据行（不含表头）。
     *
     * @param file            上传文件（支持 .xlsx/.xls）
     * @param expectedHeaders 期望的表头列名（按列顺序）
     * @return 每行一个 String[]，长度与表头一致，单元格缺失时为空串
     * @throws IOException              文件读取失败
     * @throws IllegalArgumentException 非 Excel / 空文件 / 表头与模板不一致
     */
    public static List<String[]> readDataRows(MultipartFile file, String[] expectedHeaders) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        String filename = file.getOriginalFilename();
        if (!StringUtils.hasText(filename)
                || !(filename.toLowerCase().endsWith(".xlsx") || filename.toLowerCase().endsWith(".xls"))) {
            throw new IllegalArgumentException("请上传 Excel 文件（.xlsx / .xls）");
        }

        List<String[]> rows = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int headerRowIdx = sheet.getFirstRowNum();
            Row header = sheet.getRow(headerRowIdx);
            if (header == null || header.getLastCellNum() < expectedHeaders.length) {
                throw new IllegalArgumentException("Excel 表头列数不足，请使用系统提供的导入模板");
            }
            for (int i = 0; i < expectedHeaders.length; i++) {
                String actual = cellText(header.getCell(i));
                if (!actual.equalsIgnoreCase(expectedHeaders[i])) {
                    throw new IllegalArgumentException("Excel 第 1 列表头「" + actual + "」与模板不一致（应为「"
                            + expectedHeaders[i] + "」），请使用系统提供的导入模板");
                }
            }

            for (int r = headerRowIdx + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                String[] values = new String[expectedHeaders.length];
                boolean anyText = false;
                for (int c = 0; c < expectedHeaders.length; c++) {
                    values[c] = cellText(row.getCell(c));
                    if (!values[c].isEmpty()) {
                        anyText = true;
                    }
                }
                if (anyText) {
                    rows.add(values);
                }
            }
        }
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Excel 中没有可导入的数据行（数据从第 2 行开始）");
        }
        return rows;
    }

    /** 单元格统一转文本：空→空串；数值→纯数字（去掉科学计数/小数尾巴）；日期→yyyy-MM-dd HH:mm */
    private static String cellText(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue() == null ? "" : cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue()
                            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                }
                double value = cell.getNumericCellValue();
                // 整数（如手机号/学号）以纯数字字符串返回，避免 “1.38E10” 这类形式
                yield value == Math.floor(value)
                        ? BigDecimal.valueOf(value).toBigInteger().toString()
                        : String.valueOf(value);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue().trim();
                } catch (Exception e) {
                    yield String.valueOf(cell.getNumericCellValue());
                }
            }
            default -> "";
        };
    }
}
