package sicau.policialPartyManager.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 课程字典新增/编辑请求（#32，超管维护）。
 * 名称全表唯一（应用层校验）；type 为自由文本、不预设枚举（triage 决策）。
 */
@Data
public class StudySaveRequest {

    /** 课程名（tb_study.name varchar(30)） */
    @NotBlank(message = "课程名不能为空")
    @Size(max = 30, message = "课程名长度不能超过 30")
    private String name;

    /** 类型（tb_study.type varchar(20)，自由文本，如 必修/选修/专题） */
    @NotBlank(message = "课程类型不能为空")
    @Size(max = 20, message = "课程类型长度不能超过 20")
    private String type;
}
