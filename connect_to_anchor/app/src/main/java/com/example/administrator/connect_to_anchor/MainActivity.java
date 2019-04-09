
//c4需要的数据定义在UdpServer.java中

package com.example.administrator.connect_to_anchor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StrictMode;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


import java.lang.ref.WeakReference;
import java.net.InetAddress;

public class MainActivity extends AppCompatActivity {
    private Button btnTcpConnect, btnUdpStart, btnUdpClose,btnRcvClear,btnSendClear,btnUdpSend; //定义按钮
    private TextView txtRcv,txtSend;//消息显示窗口
    private EditText editSend,editIp,editPort;//消息编辑框

    public static Context context ;

    //udp变量

    private MyHandler myHandler =   new MyHandler(this);
    private MyBroadcastReceiver myBroadcastReceiver = new MyBroadcastReceiver();
    private MyBtnClick myBtnClick = new MyBtnClick();
    private static UdpServer udpServer = null;

    //tcp变量

    private TcpClient connectThread = null;
    boolean connect=true;//连接还是断开
    boolean isConnected=false;//连接还是断开
    boolean socketIsNull=true;//连接还是断开
    public static final String fontCode="GBK";//字体编码

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //允许主线程操作

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        context = this;
        bindWidget();
        listening();//按钮监听
        bindReceiver();//绑定udp接收消息
        while (!isConnected) {
            connectOnClick();//启动app时一直尝试连接tcp服务器直到成功
        }
        udpStart();//确定连通锚点后开启udp服务器
        sendOnClick("addr");//获取锚点ip
        sendOnClick("data");//给锚点发送数据命令

    }

    //页面元素绑定

    private void bindWidget(){
        btnTcpConnect = (Button) findViewById(R.id.btnTcpConnect);
        btnUdpClose   = (Button) findViewById(R.id.btnUdpClose);
        btnUdpStart   = (Button) findViewById(R.id.btnUdpStart);
        btnUdpSend    = (Button) findViewById(R.id.btnSend);
        btnRcvClear   = (Button) findViewById(R.id.btnCleanRcv);
        btnSendClear  = (Button) findViewById(R.id.btnCleanSend);
        txtRcv        = (TextView) findViewById(R.id.txtRcv);
        txtSend       = (TextView) findViewById(R.id.txtSend);
        editIp        = (EditText) findViewById(R.id.editIp);
        editPort      = (EditText) findViewById(R.id.editPort);
        editSend      = (EditText) findViewById(R.id.editSend);

    }

    //按钮监听

    private void listening(){
        btnUdpStart.setOnClickListener(myBtnClick);
        btnUdpClose.setOnClickListener(myBtnClick);
        btnUdpSend.setOnClickListener(myBtnClick);
        btnRcvClear.setOnClickListener(myBtnClick);
        btnSendClear.setOnClickListener(myBtnClick);

        //tcp

        btnTcpConnect.setOnClickListener(myBtnClick);


    }

    //tcp连接

    private void connectOnClick() {

        if (connect == true) {//标志位 = true表示连接
            //启动连接线程
            //String ip = editIp.getText().toString();
            //String port = editPort.getText().toString();
            String ip = "192.168.100.10";//锚点tcp服务器ip
            String port = "60000";//锚点tcp服务器端口
            connectThread = new TcpClient(ip, port);
            connectThread.start();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try{
                connectThread.connectDetect();
                if (connectThread.getConnectStatus()) {
                    connect = false;//置为false
                    btnTcpConnect.setText("断开");//按钮上显示--断开
                    socketIsNull = connectThread.socketIsNull();
                    isConnected=true;
                } else {
                }
            }catch(Exception ex){
                ex.printStackTrace();
            }
        } else {//标志位 = false表示退出连接!socketIsNull
            connect = true;//置为true
            btnTcpConnect.setText("连接");//按钮上显示连接.
            try {
                connectThread.connectClose();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    //向tcp服务器发送

    private void sendOnClick(String sData) {
        try {
            //connectThread.tcpSend(editSend.getText().toString());
            //txtSend.append(editSend.getText().toString());
            connectThread.tcpSend(sData);
            txtSend.append(sData);
            connectThread.sleep(1000);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    //udp服务器开启

    private void udpStart() {
        int mPort = Integer.parseInt("51000");//主端口（本地udp服务器端口）
        int sPort = Integer.parseInt("60000");//从端口（远程udp服务器端口）
        String mIp = "";//主ip（本地udp服务器ip）
        String sIp = "192.168.100.10";//从ip（远程udp服务器ip）
        InetAddress address = null;
        try {
            LocalHostLANAddress HostLANAddress = new LocalHostLANAddress();
            address = HostLANAddress.getLocalHostLANAddress();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mIp = address.getHostAddress();//本地ip地址
        udpServer = new UdpServer(mIp, mPort, sIp, sPort);
        try {
            Thread thread = new Thread(udpServer);
            thread.start();//启动udp线程
            btnUdpStart.setEnabled(false);
            btnUdpClose.setEnabled(true);
            /*new  AlertDialog.Builder(MainActivity.this)
                    .setTitle("提示" )
                    .setMessage("ip地址为" + ip + "")
                    .setPositiveButton("确定" ,  null )
                    .show();*/
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //绑定udp消息

    private void bindReceiver() {
        IntentFilter intentFilter = new IntentFilter("udpReceiver");
        registerReceiver(myBroadcastReceiver,intentFilter);
    }

    //udp界面显示消息（接收客户端消息）

    private class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;
        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<MainActivity>(activity);
        }
        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            if (null != activity){
                switch (msg.what){
                    case 1:
                        String str = msg.obj.toString();
                        txtRcv.setText("");
                        txtRcv.append(str);
                        break;
                    case 3:
                        break;
                }
            }
        }
    }

    //udp消息类

    private class MyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String mAction = intent.getAction();
            switch (mAction){
                case "udpReceiver":
                    String msg = intent.getStringExtra("udpReceiver");
                    Message message = new Message();
                    message.what = 1;
                    message.obj = msg;
                    myHandler.sendMessage(message);
                    break;
            }
        }
    }




    //按钮行为

    private class MyBtnClick implements Button.OnClickListener{

        @Override
        public void onClick(View view) {
            switch (view.getId()){
                case R.id.btnTcpConnect:
                    if (!editIp.getText().toString().isEmpty() && !editPort.getText().toString().isEmpty()) {
                        connectOnClick();
                    } else {
                        new  AlertDialog.Builder(MainActivity.this)
                                .setTitle("提示" )
                                .setMessage("请输入ip地址和端口号" )
                                .setPositiveButton("确定" ,  null )
                                .show();
                    }
                    break;
                case R.id.btnSend:
                    sendOnClick("data");
                    break;
                case R.id.btnUdpStart:
                    udpStart();
                    break;
                case R.id.btnUdpClose:
                    final Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final Handler thread = new Handler(Looper.getMainLooper());
                            thread.post( new Runnable() {
                                @Override
                                public void run() {
                                    //关闭UDP
                                    udpServer.setUdpLife(false);
                                    while (udpServer.getLifeMsg()); //等待udp阻塞结束
                                    Looper.getMainLooper();
                                    btnUdpClose.setEnabled(false);
                                    btnUdpStart.setEnabled(true);
                                }
                            });
                        }
                    });
                    if (!btnUdpStart.isEnabled()) {
                        thread.start();
                    }
                    break;
                case R.id.btnCleanRcv:
                    txtRcv.setText("");
                    break;
                case R.id.btnCleanSend:
                    txtSend.setText("");
                    break;
            }
        }
    }

}

