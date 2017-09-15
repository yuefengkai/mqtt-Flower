package com.gaozengzhi.flower;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {

    String TAG = "Flow Tag";

    TextView txt_temp;
    TextView txt_humiture;
    Button btn_Start;

    private Timer mTimer;
    private TimerTask mTimerTask;

    final String serverUrl = "http://192.168.1.74";

    Boolean IsOpen = false;

    MQTTService mqttService;

    MyHome myHome = new MyHome();

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);//去掉标题栏

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {//4.4 全透明状态栏
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {//5.0 全透明实现
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }

        setContentView(R.layout.activity_main);

        Log.i(TAG, "onCreate: ..........");
        LoadView();

        // GetStatus();
        mTimer = new Timer();

        mTimerTask = new TimerTask() {
            @Override
            public void run() {

                final int temp = myHome.getTemperature();
                final int humidity = myHome.getHumidity();
                txt_temp.post(new Runnable() {
                    @Override
                    public void run() {
                        txt_temp.setText("" + (temp == 0 ? "..." : temp) + "° ");
                    }
                });

                txt_humiture.post(new Runnable() {
                    @Override
                    public void run() {
                        txt_humiture.setText("" + (humidity == 0 ? "..." : humidity) + "% ");
                    }
                });
            }
        };

        mTimer.schedule(mTimerTask, 1000, 2000);


        startService(new Intent(this, MQTTService.class));//启动服务

        bindService(new Intent(this, MQTTService.class), myServerConn, BIND_AUTO_CREATE); //绑定服务

    }


    //加载View
    private void LoadView() {
        txt_temp = (TextView) findViewById(R.id.txt_temp);
        txt_humiture = (TextView) findViewById(R.id.txt_humiture);
        btn_Start = (Button) findViewById(R.id.btn_start);

        btn_Start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {

                            if (IsOpen) {
                                IsOpen = false;
                                uiHandler.sendEmptyMessage(0);
                                MQTTService.publish("homebridge/from/set", "{\"name\":\"flower_door\",\"service_name\":\"flower_door\",\"characteristic\":\"CurrentDoorState\",\"value\":1}");
                            } else {

                                IsOpen = true;
                                uiHandler.sendEmptyMessage(1);
                                MQTTService.publish("homebridge/from/set", "{\"name\":\"flower_door\",\"service_name\":\"flower_door\",\"characteristic\":\"CurrentDoorState\",\"value\":0}");
                            }
                        } catch (Exception ex) {
                            Log.i(TAG, "You Find Me: .........." + ex);
                        }

                    }
                }).start();

            }
        });

        Log.i(TAG, "LoadView: ..........");
    }

    /*构造一个Handler，主要作用有：1）供非UI线程发送Message  2）处理Message并完成UI更新*/
    public Handler uiHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    btn_Start.setBackgroundResource(R.mipmap.off);
                    break;
                case 1:
                    btn_Start.setBackgroundResource(R.mipmap.on);
                    break;
                default:
                    break;

            }
            return false;
        }
    });

    ServiceConnection myServerConn = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //返回一个MsgService对象
            mqttService = ((MQTTService.MQTTServiceBinder) service).getService();
            myHome = mqttService.GetHomeStatus();

            Log.d(TAG, "----" + myHome.getHumidity());
        }
    };

    @Override
    protected void onDestroy() {
        // TODO 自动生成的方法存根
        super.onDestroy();
        unbindService(myServerConn);
    }
}
