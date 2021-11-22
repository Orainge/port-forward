package com.orainge.tools.port_forward;

import com.orainge.tools.port_forward.server.PortForwardServer;

/**
 * 端口转发类 Demo
 *
 * @author orainge
 * @since 2021/11/15
 */
public class PortForwardDemo {
    /**
     * 启动方法
     *
     * @param args [-h ip] 服务端监听 IP<br>
     *             [-p port] 服务端监听端口<br>
     *             [-dh ip] 要转发的目标 IP<br>
     *             [-dp port] 要转发的目标端口
     */
    public static void main(String[] args) {
        String listeningIp = null, targetIp = null;
        Integer listeningPort = null, targetPort = null;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("-h".equals(arg)) {
                // 监听 IP
                if (i + 1 < args.length) {
                    listeningIp = args[i + 1];
                } else {
                    throw new NullPointerException("请指定服务端监听 IP");
                }
            } else if ("-p".equals(arg)) {
                // 监听端口
                if (i + 1 < args.length) {
                    try {
                        listeningPort = Integer.parseInt(args[i + 1]);
                    } catch (Exception e) {
                        throw new NullPointerException("请填写有效的监听端口");
                    }
                } else {
                    throw new NullPointerException("请指定服务端监听端口");
                }
            } else if ("-dh".equals(arg)) {
                // 要转发的目标 IP
                if (i + 1 < args.length) {
                    targetIp = args[i + 1];
                } else {
                    throw new NullPointerException("请填写有效的目标转发 IP");
                }
            } else if ("-dp".equals(arg)) {
                // 要转发的目标端口
                if (i + 1 < args.length) {
                    try {
                        targetPort = Integer.parseInt(args[i + 1]);
                    } catch (Exception e) {
                        throw new NullPointerException("请填写有效的目标转发端口");
                    }
                } else {
                    throw new NullPointerException("请填写有效的目标转发端口");
                }
            }
        }

        // 启动监听服务
        (new PortForwardServer(listeningIp, listeningPort, targetIp, targetPort)).setAlwaysRun(true).start();
    }
}
