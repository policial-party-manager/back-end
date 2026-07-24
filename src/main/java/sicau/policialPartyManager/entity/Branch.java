package sicau.policialPartyManager.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_branch")
public class Branch {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String branchName;
    private String college;
    private Long secretaryId;
    private String description;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
