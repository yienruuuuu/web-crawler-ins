package org.example.bean.enumtype;

import lombok.Getter;

/**
 * @author Eric.Lee
 * Date: 2024/2/17
 */
@Getter
public enum LoginAccountStatusEnum {
    /**
     * 可供登入之帳號狀態
     */
    NORMAL("正常"),
    EXHAUSTED("暫時無法使用"),
    DEVIANT("異常"),
    BLOCKED("無法正常登入");

    private final String description;

    LoginAccountStatusEnum(String description) {
        this.description = description;
    }
}
