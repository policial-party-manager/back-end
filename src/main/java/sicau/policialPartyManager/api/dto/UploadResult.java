package sicau.policialPartyManager.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 文件上传结果。
 * <p>
 * url 采用项目既有前缀约定：{@code file:} 开头为应用本地存储相对路径
 * （与活动封面、公告内容的 file: 语义一致）。
 */
@Data
@AllArgsConstructor
public class UploadResult {

    /** 形如 file:thought/2026/10/uuid.pdf */
    private String url;

    /** 原始文件名（便于前端回显） */
    private String originalName;

    /** 字节数 */
    private long size;
}
