package sicau.policialPartyManager.security;

import lombok.Getter;

/**
 * 系统角色枚举 - 决定用户能访问哪些功能
 */
@Getter
public enum UserRole {
    SUPER_ADMIN("super_admin", "超级管理员"),
    BRANCH_ADMIN("branch_admin", "支部管理员"),
    STUDENT("student", "普通成员");

    private final String code;
    private final String label;

    UserRole(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
