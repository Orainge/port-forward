package com.orainge.tools.port_forward.bean;

import com.orainge.tools.port_forward.consts.PortForwardType;
import com.orainge.tools.port_forward.handler.ConnectionHandler;
import com.orainge.tools.port_forward.server.PortForwardServer;
import com.orainge.tools.port_forward.util.UUIDUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

/**
 * 端口转发连接<br>
 * [客户端->目标端口] 的实体类
 *
 * @author orainge
 * @since 2021/11/15
 */
public class PortForwardConnection {
    private static final Logger log = LoggerFactory.getLogger(PortForwardConnection.class);

    /**
     * 使用此连接的服务端
     */
    private final PortForwardServer server;

    /**
     * 连接 ID
     */
    private final String connectionId;

    /**
     * [客户端->代理] 的连接
     */
    private final Socket clientToListen;

    /**
     * [代理->目标端口] 的连接
     */
    private final Socket listenToTarget;

    /**
     * [客户端->目标端口] 的转发线程
     */
    private final PortForwardThread clientToTargetThread;

    /**
     * [目标端口->客户端] 的转发线程
     */
    private final PortForwardThread targetToClientThread;

    /**
     * 当前连接包含的两个连接 Socket 是否已经关闭
     */
    private volatile boolean isAllConnectionClosed = false;

    /**
     * 连接的信息<br>
     * [0] - [3]: 客户端 IP, 客户端端口, 客户端连接代理服务时代理服务的 IP, 客户端连接代理服务时代理服务的端口 <br>
     * [4] - [7]: 连接目标端口时代理服务的客户端 IP, 连接目标端口时代理服务的客户端端口, 目标 IP, 目标端口
     */
    private final Object[] connInfo = new Object[8];

    /**
     * 创建 [客户端->目标端口] 的端口转发连接
     *
     * @param server         转发服务端
     * @param clientToListen [客户端->代理] 的 Socket 连接
     */
    public PortForwardConnection(PortForwardServer server, Socket clientToListen) throws Exception {
        this.server = server;
        this.connectionId = generateConnectionId();

        // 保存 [客户端->代理] 的连接
        this.clientToListen = clientToListen;

        // 创建 [代理->目标端口] 的连接并保存
        this.listenToTarget = new Socket(server.getTargetIp(), server.getTargetPort());

        // 保存 [客户端->代理] 连接的信息
        InetSocketAddress ctlAddress = (InetSocketAddress) clientToListen.getRemoteSocketAddress();
        connInfo[0] = ctlAddress.getAddress().getHostAddress(); // 客户端 IP
        connInfo[1] = ctlAddress.getPort(); // 客户端端口
        connInfo[2] = clientToListen.getLocalAddress().getHostAddress(); // 代理监听 IP
        connInfo[3] = clientToListen.getLocalPort(); // 代理监听端口
        log.debug("[端口转发连接 {}] - [{}] 已连接", connectionId, PortForwardType.CLIENT_TO_LISTEN.getDescription());

        // 保存 [代理->目标端口] 连接的信息
        InetSocketAddress lttAddress = (InetSocketAddress) listenToTarget.getRemoteSocketAddress();
        connInfo[4] = listenToTarget.getLocalAddress().getHostAddress(); // 连接目标端口时代理服务的客户端 IP
        connInfo[5] = listenToTarget.getLocalPort(); // 连接目标端口时代理服务的客户端端口
        connInfo[6] = lttAddress.getAddress().getHostAddress(); // 目标 IP
        connInfo[7] = lttAddress.getPort(); // 目标端口
        log.debug("[端口转发连接 {}] - [{}] 已连接", connectionId, PortForwardType.LISTEN_TO_TARGET.getDescription());

        // 创建 [客户端->目标端口] 的转发线程
        this.clientToTargetThread = new PortForwardThread(this, PortForwardType.CLIENT_TO_TARGET);

        // 创建 [目标端口->客户端] 的转发线程
        this.targetToClientThread = new PortForwardThread(this, PortForwardType.TARGET_TO_CLIENT);

        // 启动 [客户端->目标端口] 的转发线程
        this.clientToTargetThread.start();

        // 启动 [目标端口->客户端] 的转发线程
        this.targetToClientThread.start();
    }

    /**
     * 获取连接 ID
     */
    public String getConnectionId() {
        return this.connectionId;
    }

    /**
     * 获取 [客户端->代理] 的连接
     */
    public Socket getClientToListen() {
        return this.clientToListen;
    }

    /**
     * 获取 [客户端->代理] 的连接
     */
    public Socket getListenToTarget() {
        return this.listenToTarget;
    }

    /**
     * 获取 [客户端->目标端口] 的转发线程
     */
    public PortForwardThread getClientToTargetThread() {
        return this.clientToTargetThread;
    }

    /**
     * 获取 [目标端口->客户端] 的转发线程
     */
    public PortForwardThread getTargetToClientThread() {
        return this.targetToClientThread;
    }

    /**
     * 关闭端口转发连接
     */
    public synchronized void close() {
        if (!isAllConnectionClosed) {
            // 关闭 [客户端->代理] 的连接
            try {
                if (!clientToListen.isClosed()) {
                    clientToListen.close();
                }
                log.debug("[端口转发连接 {}] - [{}] 已关闭", connectionId, PortForwardType.CLIENT_TO_LISTEN.getDescription());
            } catch (Exception e) {
                if (e instanceof SocketException) {
                    log.debug("[端口转发连接 {}] - [{}] 已关闭", connectionId, PortForwardType.CLIENT_TO_LISTEN.getDescription());
                    return;
                }
                log.error("[端口转发连接 (" + connectionId + ")] - [" + PortForwardType.CLIENT_TO_LISTEN.getDescription() + "] 关闭异常", e);
            }

            // 关闭 [代理->目标端口] 的连接
            try {
                if (!listenToTarget.isClosed()) {
                    listenToTarget.close();
                }
                log.debug("[端口转发连接 {}] - [{}] 已关闭", connectionId, PortForwardType.LISTEN_TO_TARGET.getDescription());
            } catch (Exception e) {
                if (e instanceof SocketException) {
                    return;
                }
                log.error("[端口转发连接 (" + connectionId + ")] - [" + PortForwardType.LISTEN_TO_TARGET.getDescription() + "] 关闭异常", e);
            }

            // 设置连接已经关闭
            isAllConnectionClosed = true;

            // 告诉监听服务连接已经关闭
            server.removeConnection(connectionId);

            // 执行 Handler
            ConnectionHandler handler = server.getConnectionHandler();
            if (handler != null) {
                handler.afterClosed(this);
            }
        }
    }

    /**
     * 任意一个连接是否已经断开
     *
     * @return true: 连接已断开 false; 连接未断开
     */
    public synchronized boolean isClosed() {
        // 如果连接已经关闭，就直接返回
        if (isAllConnectionClosed) {
            return true;
        }

        // 判断连接是否断开
        boolean a = listenToTarget.isClosed() || !listenToTarget.isConnected();
        boolean b = clientToListen.isClosed() || !clientToListen.isConnected();

        if (a || b) {
            // 断开连接
            close();
        }

        // 返回结果
        return isAllConnectionClosed;
    }

    /**
     * 创建连接 ID<br>
     * 可以重写此方法自定义获取 connectionId 的方法
     */
    protected String generateConnectionId() {
        return UUIDUtil.getShortUuid();
    }

    /**
     * 输出连接 ID 和 [客户端->代理] 和 [代理->目标端口] 的连接状态
     */
    @Override
    public String toString() {
        String clientToListenLog = "[" + connInfo[0] + ":" + connInfo[1] + "->" + connInfo[2] + ":" + connInfo[3] + "]";
        String listenToTargetLog = "[" + connInfo[4] + ":" + connInfo[5] + "->" + connInfo[6] + ":" + connInfo[7] + "]";
        return "[端口转发连接 " + connectionId + "] - " +
                "[客户端->代理] " + clientToListenLog + " - " + (clientToListen.isClosed() ? "断开" : "连接") +
                ", [代理->目标端口] " + listenToTargetLog + " - " + (listenToTarget.isClosed() ? "断开" : "连接");
    }
}
