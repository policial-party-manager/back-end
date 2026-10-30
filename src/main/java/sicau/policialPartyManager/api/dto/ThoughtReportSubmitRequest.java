package sicau.policialPartyManager.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 思想汇报提交请求（成员侧）。
 * <p>
 * 审阅人不在请求内：由服务端按提交人支部的秘书自动派定（#33 决策）。
 */
@Data
public class ThoughtReportSubmitRequest {

    /** 标题（tb_thought_report.title varchar(100)） */
    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题长度不能超过 100")
    private String title;

    /** 正文 */
    @NotBlank(message = "汇报内容不能为空")
    private String content;

    /** 附件相对路径（先调 /api/v4/uploads 获得，形如 file:thought/...；可空） */
    @Size(max = 50, message = "附件路径长度不能超过 50")
    private String file;
}
