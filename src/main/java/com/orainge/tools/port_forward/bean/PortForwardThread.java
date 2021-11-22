package com.orainge.tools.port_forward.bean;

import com.orainge.tools.port_forward.consts.PortForwardType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

/**
 * 转发连接线程
 *
 * @author orainge
 * @since 2021/11/15
 */
public class PortForwardThread extends Thread {
    private static final Logger log = LoggerFactory.getLogger(PortForwardThread.class);

    /**
     * 线程所属连接
     */
    private final PortForwardConnection connection;

    /**
     * 来源 Socket
     */
    private Socket sourceSocket;

    /**
     * 目标 Socket
     */
    private Socket targetSocket;

    /**
     * 转发类型
     */
    private final PortForwardType type;

    public PortForwardThread(PortForwardConnection connection, PortForwardType type) {
        this.connection = connection;
        this.type = type;

        // 设置连接
        if (PortForwardType.CLIENT_TO_TARGET.equals(type)) {
            // [客户端->被转发端口] 连接
            this.sourceSocket = connection.getClientToListen();
            this.targetSocket = connection.getListenToTarget();
        } else if (PortForwardType.TARGET_TO_CLIENT.equals(type)) {
            // [被转发端口->客户端] 连接
            this.sourceSocket = connection.getListenToTarget();
            this.targetSocket = connection.getClientToListen();
        }
    }

    // in: code 1 out: code: 2
    @Override
    @SuppressWarnings("all")
    public void run() {
        boolean isDebugEnabled = log.isDebugEnabled();

        try (InputStream in = sourceSocket.getInputStream();
             OutputStream out = targetSocket.getOutputStream();
        ) {
            byte[] buffer = new byte[1024];
            while (true) {
                // 如果任意一方连接关闭，就退出 while 循环
                if (connection.isClosed()) {
                    // 写入缓冲区剩余的数据
                    writeOutputStream(out, buffer);

                    // 退出循环
                    break;
                }

                int len = 0;
                try {
                    // 读入数据
                    len = in.read(buffer);

                    if (len == -1) {
                        // 写入缓冲区剩余的数据
                        writeOutputStream(out, buffer);

                        // 退出循环
                        break;
                    }
                } catch (Exception e) {
                    if (!(e instanceof SocketException)) {
                        // 读取异常
                        if (isDebugEnabled) {
                            log.debug("[端口转发线程] - [" + type.getDescription() + " - " + PortForwardType.getSubType(type, "1").getDescription() +
                                    "] 从写入流 [in] 读取异常: 连接" + (sourceSocket.isClosed() ? "断开" : "正常"), e);
                        }
                    }

                    break;
                }

                // 写入数据
                if (!(writeOutputStream(out, buffer))) {
                    break;
                }

                // 等待 300 毫秒
                Thread.sleep(300);
            }
        } catch (SocketException e) {
            log.debug("[端口转发线程] - [" + type.getDescription() + "] 连接异常关闭");
        } catch (Exception e) {
            log.error("[端口转发线程] - [" + type.getDescription() + "] 转发异常", e);
        }

        // 退出 while, 表示结束连接，关闭两个 Socket
        connection.close();
    }

    private boolean writeOutputStream(OutputStream out, byte[] buffer) {
        try {
            out.write(buffer);
            out.flush();
            return true;
        } catch (Exception e) {
            if (!(e instanceof SocketException)) {
                // 读取异常
                if (log.isDebugEnabled()) {
                    // 获取 SocketTransferType
                    log.debug("[端口转发线程] - [" + type.getDescription() + " - " + PortForwardType.getSubType(type, "2").getDescription() +
                            "] 写入到输出流 [out] 异常: 连接" + (targetSocket.isClosed() ? "断开" : "正常"), e);
                }
            }

            return false;
        }
    }
}