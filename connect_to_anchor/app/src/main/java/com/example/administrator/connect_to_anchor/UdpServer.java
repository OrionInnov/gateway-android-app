
//udp服务器端

package com.example.administrator.connect_to_anchor;

import android.content.Intent;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import android.util.Base64;

public class UdpServer implements Runnable {
    private String mIp = null;//定义主ip（本地udp服务器ip）
    private String sIp = null;//定义从ip（远程udp服务器ip）
    private String lIp = null;//定义锚点eth0 ip地址
    private int mPort = 0;//定义主端口（本地udp服务器端口）
    private int sPort = 0;//定义从端口（远程udp服务器端口）
    private DatagramPacket dpRcv = null,dpSend = null;//定义包接收/发送
    private static DatagramSocket ds = null;//定义udp socket类
    private InetSocketAddress inetSocketAddress = null;//定义socket地址
    private byte[] msgRcv = new byte[1024];//定义消息接收缓冲区
    private boolean udpLife = true;     //udp生命线程
    private boolean udpLifeOver = true; //生命结束标志，false为结束



    public UdpServer(String mIpVal, int mPortVal, String sIpVal, int sPortVal) {
        this.mIp = mIpVal;
        this.mPort = mPortVal;
        this.sIp = sIpVal;
        this.sPort = sPortVal;
    }

    //设定接收超时

    private void SetSoTime(int ms) throws SocketException {
        ds.setSoTimeout(ms);
    }

    //返回udp生命线程因子是否存活
    public boolean isUdpLife(){
        if (udpLife){
            return true;
        }
        return false;
    }

    //返回具体线程生命信息是否完结
    public boolean getLifeMsg(){
        return udpLifeOver;
    }

    //更改UDP生命线程因子
    public void setUdpLife(boolean b){
        udpLife = b;
    }

    //返回接收到的锚点ip

    public String  getLocalIp(){
        return lIp;
    }

    //二进制bytes转base16字符串

    public static String bytes2HexString(byte[] b,int alength ) {
        String ret = "";
        for (int i = 0; i < b.length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            ret += hex;
        }
        ret = ret.substring(0,alength*2);
        return ret;
    }

    @Override
    public void run() {
        inetSocketAddress = new InetSocketAddress(mIp, mPort);//本地地址
        try {
            ds = new DatagramSocket(inetSocketAddress);//启动udp服务器
            Log.i("SocketInfo", "UDP服务器已经启动");
            SetSoTime(100);//设置超时，不需要可以删除
        } catch (SocketException e) {
            e.printStackTrace();
        }

        dpRcv = new DatagramPacket(msgRcv, msgRcv.length);//接收包

        //持续监听并转发锚点的消息到远程udp服务器

        while (udpLife) {
            try {
                Log.i("SocketInfo", "UDP监听中");
                ds.receive(dpRcv);//接收消息
                String ipData = new String(dpRcv.getData(), dpRcv.getOffset(), dpRcv.getLength());//将接收到的消息转换成对应长度的字符串

                //判断从锚点接收到的消息是否为ip地址

                if ( ipData.startsWith("192.")) {
                    lIp = ipData;//保存ip地址
                    Intent intent =new Intent();
                    intent.setAction("udpReceiver");
                    intent.putExtra("udpReceiver",ipData);
                    MainActivity.context.sendBroadcast(intent);//将消息发送给主界面
                } else {
                    byte[] anchorPosByte = new byte[dpRcv.getLength()];//定义接收到的数据
                    System.arraycopy(dpRcv.getData(), 0, anchorPosByte, 0, dpRcv.getLength());//截取接收到的对应长度bytes
                    String anchorPos = Base64.encodeToString(anchorPosByte,Base64.DEFAULT);//将bytes转换成base64字符串
                    String dateJson ="{\"pos_64\":\"" + anchorPos + "\",\"num\":\"" + lIp + "\"}";//将ip和base64数据放入json字符串
                    byte[] bufJson = dateJson.getBytes();//json转换成bytes传输
                    InetAddress sIpAddress = InetAddress.getByName(sIp);//远程udp服务器ip
                    dpSend = new DatagramPacket(bufJson,bufJson.length,sIpAddress,sPort);//发送包
                    ds.send(dpSend);//发送至远程udp服务器
                    String str = bytes2HexString(dpRcv.getData(), dpRcv.getLength());//锚点数据转换base16字符串
                    String string = new String(dpRcv.getData(), dpRcv.getOffset(), dpRcv.getLength());
                    Log.i("SocketInfo", "收到信息：" + string);
                    Intent intent =new Intent();
                    intent.setAction("udpReceiver");
                    intent.putExtra("udpReceiver",str);
                    MainActivity.context.sendBroadcast(intent);//将锚点数据发送给主界面
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        ds.close();
        Log.i("SocketInfo","UDP监听关闭");
        //udp生命结束
        udpLifeOver = false;
    }
}
