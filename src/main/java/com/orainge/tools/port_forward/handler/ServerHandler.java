package com.orainge.tools.port_forward.handler;

import com.orainge.tools.port_forward.server.PortForwardServer;

/**
 * 端口转发服务 Handler
 *
 * @author orainge
 * @since 2021/11/19
 */
public interface ServerHandler {
    /**
     * 当连接成功后
     *
     * @param server 端口转发服务
     */
    void afterStart(PortForwardServer server);


    /**
     * 当连接停止后
     *
     * @param server 端口转发服务
     */
    void afterStop(PortForwardServer server);

    /**
     * 当连接停止后
     *
     * @param server 端口转发服务
     */
    void onError(PortForwardServer server, Exception e);
}
