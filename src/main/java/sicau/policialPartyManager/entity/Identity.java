package sicau.policialPartyManager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("tb_identity")
public class Identity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String identityName;
    private Integer level;
    private String description;
}
