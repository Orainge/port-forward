package com.orainge.tools.port_forward.consts;

/**
 * Socket 转发的类型
 *
 * @author orainge
 * @since 2021/11/15
 */
public enum PortForwardType {
    UNKNOWN("-1", "-1", "未知类型"),
    CLIENT_TO_TARGET("0", "1", "客户端->目标端口"),
    CLIENT_TO_LISTEN("1", "1", "客户端->代理"),
    LISTEN_TO_TARGET("1", "2", "代理->目标端口"),
    TARGET_TO_CLIENT("0", "2", "目标端口->客户端"),
    TARGET_TO_LISTEN("2", "1", "目标端口->代理"),
    LISTEN_TO_CLIENT("2", "2", "代理->客户端");

    /**
     * 转发类型分组码
     */
    private final String groupCode;

    /**
     * 转发类型代码
     */
    private final String code;

    /**
     * 转发类型描述
     */
    private final String description;

    PortForwardType(String groupCode, String code, String description) {
        this.groupCode = groupCode;
        this.code = code;
        this.description = description;
    }

    public String getGroupCode() {
        return groupCode;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据 [父级转发类型] 和 [转发类型代码] 获取 Type
     *
     * @param parentType 转发类型分组码
     * @param code       转发类型代码
     * @return type 对象
     */
    public static PortForwardType getSubType(PortForwardType parentType, String code) {
        // 初始化默认值
        PortForwardType defaultType = PortForwardType.UNKNOWN;

        // 异常情况，返回默认值
        if (code == null || code.equals("") || defaultType.equals(parentType)) {
            return defaultType;
        }

        for (PortForwardType type : PortForwardType.values()) {
            if (type.getGroupCode().equals(parentType.code) && type.code.equals(code)) {
                // 返回对应的子 type
                return type;
            }
        }

        // 没有找到对应的子 type，返回默认值
        return PortForwardType.UNKNOWN;
    }
}
