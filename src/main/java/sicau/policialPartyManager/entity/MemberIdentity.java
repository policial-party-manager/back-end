package sicau.policialPartyManager.entity;

import lombok.Getter;

/**
 * 党员发展阶段（党员身份）枚举 - 与系统角色独立
 */
@Getter
public enum MemberIdentity {
    ORDINARY("ordinary", "普通学生"),
    APPLICANT("applicant", "入党申请人"),
    ACTIVIST("activist", "积极分子"),
    DEVELOPMENT("development", "发展对象"),
    PROBATIONARY("probationary", "预备党员"),
    FULL("full", "正式党员");

    private final String code;
    private final String label;

    MemberIdentity(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
