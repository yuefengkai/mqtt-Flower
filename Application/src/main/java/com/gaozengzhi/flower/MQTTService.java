package com.gaozengzhi.flower;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;


/**
 * MQTT长连接服务
 *
 * @author 一口仨馍 联系方式 : yikousamo@gmail.com
 * @version 创建时间：2016/9/16 22:06
 */
public class MQTTService extends Service {

    public static final String TAG = MQTTService.class.getSimpleName();

    private static MqttAndroidClient client;
    private MqttConnectOptions conOpt;

    private String host = "tcp://192.168.1.249:1883";
    private static String myTopic = "homebridge/from/set";
    private String clientId = "test11111111111111";


    public com.gaozengzhi.flower.MyHome myHome = new MyHome();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        init();
        return super.onStartCommand(intent, flags, startId);
    }

    public static void publish(String topic, String msg) {
        if (topic.equals("")) {
            topic = myTopic;
        }

        Integer qos = 0;
        Boolean retained = false;
        try {

            Log.i(TAG, topic + " " + msg);
            client.publish(topic, msg.getBytes(), qos.intValue(), retained.booleanValue());
        } catch (MqttException e) {
            Log.e(TAG, "Exception Occured", e);
            e.printStackTrace();

        }
    }

    private void init() {
        // 服务器地址（协议+地址+端口号）
        String uri = host;
        client = new MqttAndroidClient(this, uri, clientId);
        // 设置MQTT监听并且接受消息
        client.setCallback(mqttCallback);

        conOpt = new MqttConnectOptions();
        // 清除缓存
        conOpt.setCleanSession(true);
        // 设置超时时间，单位：秒
        conOpt.setConnectionTimeout(10);
        // 心跳包发送间隔，单位：秒
        conOpt.setKeepAliveInterval(20);
        // 用户名
        // last will message
        boolean doConnect = true;

        if (doConnect) {
            doClientConnection();

        }

    }

    @Override
    public void onDestroy() {
        try {
            client.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    /**
     * 连接MQTT服务器
     */
    private void doClientConnection() {
        if (!client.isConnected() && isConnectIsNomarl()) {
            try {
                client.connect(conOpt, null, iMqttActionListener);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }

    }

    // MQTT是否连接成功
    private IMqttActionListener iMqttActionListener = new IMqttActionListener() {

        @Override
        public void onSuccess(IMqttToken arg0) {
            Log.i(TAG, "连接成功 ");
            try {
                // 订阅myTopic话题
                client.subscribe("homebridge/from/response", 0);

                publish("homebridge/to/get", "{\"name\": \"out_temp\"}");
                publish("homebridge/to/get", "{\"name\": \"out_humidity\"}");
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onFailure(IMqttToken arg0, Throwable arg1) {
            arg1.printStackTrace();
            // 连接失败，重连
        }
    };

    // MQTT监听并且接受消息
    private MqttCallback mqttCallback = new MqttCallback() {

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {

            String str1 = new String(message.getPayload());

            JSONObject jsonObject = new JSONObject(str1);

            Log.i(TAG, "messageArrived:" + str1);
            //Log.i(TAG, str2);

            //messageArrived:{"name":"out_temp","service_name":"temperature","characteristic":"CurrentTemperature","value":"35"}
            //messageArrived:{"name":"out_humidity","service_name":"humidity","characteristic":"CurrentRelativeHumidity","value":"54"}


            //"out_temp"]["characteristics"]["temperature"]["CurrentTemperature"]
            if (jsonObject.has("out_temp")) {
                int temp = jsonObject.getJSONObject("out_temp").getJSONObject("characteristics").getJSONObject("temperature").getInt("CurrentTemperature");
                myHome.setTemperature(temp);
            }

            //s["out_humidity"]["characteristics"]["humidity"]["CurrentRelativeHumidity"]
            if (jsonObject.has("out_humidity")) {
                int humidty = jsonObject.getJSONObject("out_humidity").getJSONObject("characteristics").getJSONObject("humidity").getInt("CurrentRelativeHumidity");
                myHome.setHumidity(humidty);
            }

//
//            if (jsonObject.getString("name").equals(("out_temp"))){
//                if (jsonObject.getString("characteristic").equals(("CurrentTemperature"))){
//                    myHome.setTemperature(jsonObject.getInt("value"));
//                }
//
//            }
//
//            if (jsonObject.getString("name").equals(("out_humidity"))){
//                if (jsonObject.getString("characteristic").equals(("CurrentRelativeHumidity"))){
//                    myHome.setHumidity(jsonObject.getInt("value"));;
//                }
//            }

        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken arg0) {

        }

        @Override
        public void connectionLost(Throwable arg0) {
            // 失去连接，重连
        }
    };

    /**
     * 判断网络是否连接
     */
    private boolean isConnectIsNomarl() {
        ConnectivityManager connectivityManager = (ConnectivityManager) this.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if (info != null && info.isAvailable()) {
            String name = info.getTypeName();
            Log.i(TAG, "MQTT当前网络名称：" + name);
            return true;
        } else {
            Log.i(TAG, "MQTT 没有可用网络");
            return false;
        }
    }

    /**
     * 返回一个Binder对象
     */
    @Override
    public IBinder onBind(Intent intent) {
        return new MQTTServiceBinder();
    }

    public class MQTTServiceBinder extends Binder {
        /**
         * 获取当前Service的实例
         *
         * @return
         */
        public MQTTService getService() {
            return MQTTService.this;
        }
    }

    public com.gaozengzhi.flower.MyHome GetHomeStatus() {
        return myHome;
    }
}


