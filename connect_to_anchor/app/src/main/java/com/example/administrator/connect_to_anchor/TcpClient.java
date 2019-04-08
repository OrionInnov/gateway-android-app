package com.example.administrator.connect_to_anchor;

//tcp客户端

import java.io.*;
import java.net.*;


public class TcpClient extends Thread {//继承Thread

    private OutputStream outputStream=null;//定义输出流
    public static final String fontCode="GBK";
    private boolean isConnected  = false;//定义tcp socket连接状态

    Socket socket = null;//定义socket

    private String mIp = null;
    private String mPort = "0";
    public TcpClient(String mIpVal, String mPortVal) {
        this.mIp = mIpVal;
        this.mPort = mPortVal;
    }

    public boolean getConnectStatus() {
        return isConnected;
    }

    public boolean socketIsNull() {
        boolean a = ((null != socket) ? false:true);
        return a;
    }

    public void connectDetect() {
        try {
            socket.sendUrgentData(0xFF);
            isConnected = true;
        } catch (Exception e) {

            e.printStackTrace();
        }
    }
    public void connectClose() {
        try {
            socket.close();//关闭连接
            socket = null;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void tcpSend(String editSend) {
        try {
            //获取输出流
            outputStream = socket.getOutputStream();
            //发送数据
            outputStream.write(editSend.getBytes(fontCode));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

        }
    }

    public void run() {//重写run方法
        try {
            if (socket == null) {//如果已经连接上了，就不再执行连接程序
                int port =Integer.valueOf(mPort);//获取端口号
                socket = new Socket();
                LocalHostLANAddress HostLANAddress = new LocalHostLANAddress();
                InetAddress address = HostLANAddress.getLocalHostLANAddress();
                String localIp = address.getHostAddress();//获取本地ip
                SocketAddress a = new InetSocketAddress(localIp, 50000);//定义本地ip和端口号
                SocketAddress b = new InetSocketAddress(mIp, port);//定义锚点ip和端口号
                socket.setReuseAddress(true);//允许多个DatagramSocket绑定到相同的IP地址和端口（解决tcp断开后重连本地端口未及时释放被占用的问题）
                socket.bind(a);//绑定本地ip和端口以 便被锚点识别
                socket.connect(b);//连接锚点tcp服务器
            }
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}



