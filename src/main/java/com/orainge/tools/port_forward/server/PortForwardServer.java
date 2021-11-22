package com.orainge.tools.port_forward.server;

import com.orainge.tools.port_forward.bean.PortForwardConnection;
import com.orainge.tools.port_forward.handler.ConnectionHandler;
import com.orainge.tools.port_forward.handler.ServerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * 端口转发服务端
 *
 * @author orainge
 * @since 2021/11/15
 */
public class PortForwardServer {
    private static final Logger log = LoggerFactory.getLogger(PortForwardServer.class);

    /**
     * 监听的 IP 地址 (空代表任意地址 [0.0.0.0])
     */
    private final String listeningIp;

    /**
     * 本机监听的端口号
     */
    private final int listeningPort;

    /**
     * 要转发的目标 IP 地址
     */
    private final String targetIp;

    /**
     * 要转发的目标端口号
     */
    private final int targetPort;

    /**
     * 端口转发连接的 Handler<br>
     * 默认为 null，如果需要 Handler，重写构造函数即可
     */
    private final ConnectionHandler connectionHandler = initConnectionHandler();

    /**
     * 端口转发服务的 Handler<br>
     * 默认为 null，如果需要 Handler，重写构造函数即可
     */
    private final ServerHandler serverHandler = initServerHandler();

    /**
     * 是否保持监听服务一直启动<br>
     * 即监听服务抛出异常后能自动重启
     */
    private boolean alwaysRun = false;

    /**
     * 服务器监听的线程
     */
    private volatile Thread serverThread = null;

    /**
     * 转发连接对象容器<br>
     * {connectionId: 转发连接对象}
     */
    private volatile Map<String, PortForwardConnection> connContainer = null;

    /**
     * 服务器是否启用
     */
    private volatile boolean isServerEnabled = false;

    /**
     * 构造函数
     *
     * @param listeningIp   监听的 IP 地址 (空代表任意地址 [0.0.0.0])
     * @param listeningPort 本机监听的端口号
     * @param targetIp      要转发的目标 IP 地址
     * @param targetPort    要转发的目标端口号
     */
    public PortForwardServer(String listeningIp, Integer listeningPort, String targetIp, Integer targetPort) {
        // 初始化监听 IP
        if (listeningIp == null || "".equals(listeningIp)) {
            // IP 地址为空，监听任意地址 [0.0.0.0]
            this.listeningIp = "0.0.0.0";
            log.info("[端口转发服务] - 未设置监听 IP，默认监听 0.0.0.0");
        } else {
            this.listeningIp = listeningIp;
        }

        // 初始化监听端口
        if (listeningPort == null) {
            throw new NullPointerException("监听端口不能为空");
        } else {
            this.listeningPort = listeningPort;
        }

        // 初始化要转发的目标 IP 地址
        if (targetIp == null || "".equals(targetIp)) {
            throw new NullPointerException("要转发的目标 IP 地址不能为空");
        } else {
            this.targetIp = targetIp;
        }

        // 初始化要转发的目标端口号
        if (targetPort == null) {
            throw new NullPointerException("要转发的目标端口号不能为空");
        } else {
            this.targetPort = targetPort;
        }
    }

    /**
     * 开启服务端监听
     */
    public synchronized void start() {
        PortForwardServer server = this;

        if (!isServerEnabled && this.serverThread == null) {
            // 标识该服务器已经开启
            isServerEnabled = true;

            // 初始化转发连接对象容器
            connContainer = new HashMap<>();

            // 创建线程对象
            serverThread = new Thread(() -> {
                Exception serverException = null;

                try (
                        ServerSocket listeningServerSocket = new ServerSocket(); // 启动端口监听
                ) {
                    // 绑定监听的 IP 和端口
                    listeningServerSocket.bind(new InetSocketAddress(listeningIp, listeningPort));

                    // 执行 Handler 的方法
                    if (serverHandler != null) {
                        serverHandler.afterStart(server);
                    }

                    // 输出日志
                    log.info("[端口转发服务] - 已启动 [{}:{}]", listeningIp, listeningPort);

                    while (isServerEnabled) {
                        // 阻塞，等待客户端的连接
                        // 客户端连接的 Socket 连接对象
                        Socket sourceSocket = listeningServerSocket.accept();

                        // 服务器已关闭，关闭连接
                        if (!isServerEnabled) {
                            // 关闭当前客户端的连接和服务端的监听
                            sourceSocket.close();
                            listeningServerSocket.close();

                            // 退出循环
                            break;
                        }

                        // 创建多线程处理连接
                        new Thread(() -> {
                            PortForwardConnection connection = null;

                            try {
                                // 创建端口转发的连接
                                connection = new PortForwardConnection(server, sourceSocket);

                                // 保存创建的连接
                                connContainer.put(connection.getConnectionId(), connection);

                                // 指定 Handler 的方法
                                if (connectionHandler != null) {
                                    connectionHandler.afterConnected(connection);
                                }

                                // 输出日志
                                log.debug("[端口转发服务] - 连接成功 [{}]", connection);
                            } catch (Exception e) {
                                // 输出日志
                                log.error("[端口转发服务] - 连接失败", e);

                                // 执行 Handler 的方法
                                if (connectionHandler != null) {
                                    connectionHandler.onError(connection, e);
                                }
                            }
                        }).start();
                    }

                    log.info("[端口转发服务] - 已关闭转发服务");
                } catch (Exception e) {
                    log.error("[端口转发服务] - 创建监听服务失败", e);

                    // 服务启动失败，更新状态值
                    synchronized (server) {
                        isServerEnabled = false;
                    }

                    // 执行 Handler 的方法
                    if (serverHandler != null) {
                        serverHandler.onError(server, e);
                    }

                    serverException = e;
                }

                // 关闭服务器
                stop();

                // 执行 Handler 的方法
                if (serverHandler != null) {
                    serverHandler.afterStop(server);
                }

                // 如果需要保活监听服务，则重启服务
                if (alwaysRun) {
                    // 如果是端口监听异常，则退出服务
                    if (serverException instanceof BindException) {
                        return;
                    }

                    // 如果是手动退出的，就不自启动
                    if (serverThread == null && serverException != null) {
                        return;
                    }

                    // 重启服务
                    server.start();
                }
            });

            // 开启线程
            serverThread.start();
        }
    }

    /**
     * 关闭端口转发服务
     */
    public synchronized void stop() {
        if (isServerEnabled) {
            // 关闭所有已建立的连接（此方法已修改服务器状态）
            closeAllConnection();

            // 重置线程对象
            serverThread = null;
        }
    }

    /**
     * 关闭指定连接 ID 的连接
     *
     * @param id 连接 ID
     */
    public synchronized void closeConnection(String id) {
        if (isServerEnabled && connContainer != null) {
            // 移除连接
            PortForwardConnection connection = connContainer.remove(id);

            if (connection != null) {
                // 关闭连接
                connection.close();
            }
        }
    }

    /**
     * 移除开指定连接 ID 的连接
     *
     * @param id 连接 ID
     */
    public synchronized void removeConnection(String id) {
        if (isServerEnabled && connContainer != null) {
            PortForwardConnection connection = connContainer.remove(id);
            log.debug("[端口转发服务] - 连接已移除 [{}]", connection);
        }
    }

    /**
     * 关闭所有已建立的连接
     */
    public synchronized void closeAllConnection() {
        if (isServerEnabled) {
            for (PortForwardConnection conn : connContainer.values()) {
                conn.close();
            }

            // 移除所有的元素
            connContainer = new HashMap<>();

            // 修改服务器状态
            isServerEnabled = false;
        }
    }

    /**
     * 初始化端口转发 Handler 实例类<br>
     * 通过重写实现自定义的 Handler
     */
    protected ConnectionHandler initConnectionHandler() {
        return null;
    }

    /**
     * 初始化端口转发服务的 Handler 实例类<br>
     * 通过重写实现自定义的 Handler
     */
    protected ServerHandler initServerHandler() {
        return null;
    }

    public String getListeningIp() {
        return listeningIp;
    }

    public int getListeningPort() {
        return listeningPort;
    }

    public String getTargetIp() {
        return targetIp;
    }

    public int getTargetPort() {
        return targetPort;
    }

    public ConnectionHandler getConnectionHandler() {
        return connectionHandler;
    }

    public ServerHandler getServerHandler() {
        return serverHandler;
    }

    public boolean isAlwaysRun() {
        return alwaysRun;
    }

    public boolean isServerEnabled() {
        return isServerEnabled;
    }

    public PortForwardServer setAlwaysRun(boolean alwaysRun) {
        this.alwaysRun = alwaysRun;
        return this;
    }
}
