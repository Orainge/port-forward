package com.orainge.tools.port_forward.handler;

import com.orainge.tools.port_forward.bean.PortForwardConnection;

/**
 * 端口转发 Handler
 *
 * @author orainge
 * @since 2021/11/19
 */
public interface ConnectionHandler {
    /**
     * 当连接成功后
     *
     * @param connection 转发连接
     */
    void afterConnected(PortForwardConnection connection);

    /**
     * 当连接关闭后
     *
     * @param connection 转发连接
     */
    void afterClosed(PortForwardConnection connection);


    /**
     * 当连接失败后
     *
     * @param connection 转发连接
     */
    void onError(PortForwardConnection connection, Exception e);
}
