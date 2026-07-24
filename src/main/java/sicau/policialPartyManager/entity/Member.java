package sicau.policialPartyManager.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_member")
public class Member {
    @TableId
    private String studentNo;
    private String name;
    private String gender;
    private String college;
    private String grade;
    private String major;
    private String className;
    private Long branchId;
    private Long identityId;
    private String phone;
    private String email;
    private String avatar;
    private Integer status;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
