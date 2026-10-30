package sicau.policialPartyManager.api.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 活动签到请求（成员侧）。
 * device / location / remark 可空：Web 端通常拿不到设备号与定位。
 */
@Data
public class SignInRequest {

    /** 活动id（以路径参数为准，请求体可省略） */
    private Long activityId;

    /** 签到设备（web/app/mini 等，可空） */
    @Size(max = 10, message = "签到设备长度不能超过 10")
    private String device;

    /** 地点（WKT 格式，如 POINT(103.96 30.57)，可空） */
    private String location;

    /** 备注（可空） */
    @Size(max = 20, message = "备注长度不能超过 20")
    private String remark;
}
