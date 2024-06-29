package com.olh.mqtttest

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Binder
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.util.Log
import org.eclipse.paho.mqttv5.client.IMqttToken
import org.eclipse.paho.mqttv5.client.MqttActionListener
import org.eclipse.paho.mqttv5.client.MqttAsyncClient
import org.eclipse.paho.mqttv5.client.MqttCallback
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence
import org.eclipse.paho.mqttv5.common.MqttException
import org.eclipse.paho.mqttv5.common.MqttMessage
import org.eclipse.paho.mqttv5.common.packet.MqttProperties

class MyMqttService : Service() {
    var HOST: String = "tcp://175.178.156.231:1883" //服务器地址（协议+地址+端口号）
    var USERNAME: String = "" //用户名
    var PASSWORD: String = "" //密码

    @SuppressLint("MissingPermission")
    var CLIENTID: String = "mqtt_android" //客户端ID，一般以客户端唯一标识符表示

    private var mMqttConnectOptions: MqttConnectionOptions? = null


    override fun onBind(intent: Intent): IBinder? {
        // TODO: Return the communication channel to the service.
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        init()
        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * 初始化
     */
    private fun init() {
        Log.d(TAG, "mqtt service init fun")
        val serverURI = HOST //服务器地址（协议+地址+端口号）
        try {
            mqttAndroidClient = MqttAsyncClient(serverURI, CLIENTID, MemoryPersistence())
        } catch (e: MqttException) {
            throw RuntimeException(e)
        }
        mqttAndroidClient!!.setCallback(mqttCallback)
        mMqttConnectOptions = MqttConnectionOptions()
        mMqttConnectOptions!!.isCleanStart = true //设置是否清除缓存
        mMqttConnectOptions!!.connectionTimeout = 10 //设置超时时间，单位：秒
        mMqttConnectOptions!!.keepAliveInterval = 60 //设置心跳包发送间隔，单位：秒
        mMqttConnectOptions!!.userName = USERNAME //设置用户名
        mMqttConnectOptions!!.password = PASSWORD.toByteArray() //设置密码

        // last will message
        var doConnect = true
        val message = "last will message"
        val topic = PUBLISH_TOPIC

        val MqttMessage = MqttMessage(message.toByteArray())

        if ((message != "") || (topic != "")) {
            // 最后的遗嘱
            try {
                mMqttConnectOptions!!.setWill(topic, MqttMessage)
            } catch (e: Exception) {
                Log.i(TAG, "Exception Occured", e)
                doConnect = false
                iMqttActionListener.onFailure(null, e)
            }
        }
        if (doConnect) {
            doClientConnection()
        }
    }

    /**
     * 初始化
     */
    private val mqttCallback: MqttCallback = object : MqttCallback {
        override fun disconnected(disconnectResponse: MqttDisconnectResponse) {
        }

        override fun mqttErrorOccurred(exception: MqttException) {
        }

//        @Throws(Exception::class)
//        override fun messageArrived(topic: String, message: MqttMessage) {
//            Log.i(TAG, "收到消息： " + String(message.payload))
//            val intent = Intent(action)
//            val bundle = Bundle()
//            bundle.putString("MQTT_RevMsg", String(message.payload))
//            intent.putExtras(bundle)
//            sendBroadcast(intent)
//        }

        @Throws(Exception::class)
        override fun messageArrived(topic: String, message: MqttMessage) {
            Log.i(TAG, "收到消息：" + String(message.payload))
            val intent = Intent("com.olh.hypertool") // 使用与filter相同的action字符串
            intent.putExtra("MQTT_RevMsg", String(message.payload)) // 使用putExtra添加额外数据，无需Bundle

            // 发送非粘性广播
            sendBroadcast(intent)
        }

        override fun deliveryComplete(token: IMqttToken) {
        }

        override fun connectComplete(reconnect: Boolean, serverURI: String) {
        }

        override fun authPacketArrived(reasonCode: Int, properties: MqttProperties) {
        }
    }

    /**
     * MQTT是否连接成功的监听
     */
    private val iMqttActionListener: MqttActionListener = object : MqttActionListener {
        override fun onSuccess(arg0: IMqttToken) {
            Log.i(TAG, "连接成功 ")
            try {
                mqttAndroidClient!!.subscribe(SUBSCRIVE_TOPIC, 2) //订阅主题，参数：主题、服务质量
            } catch (e: MqttException) {
                e.printStackTrace()
            }
        }

        override fun onFailure(arg0: IMqttToken, arg1: Throwable) {
            arg1.printStackTrace()
            Log.i(TAG, "连接失败 ")
            doClientConnection() //连接失败，重连（可关闭服务器进行模拟）
        }
    }

    init {
        Log.d(TAG, "MyMqttService")
    }

    /**
     * 连接MQTT服务器
     */
    private fun doClientConnection() {
        if (!mqttAndroidClient!!.isConnected && isConnectIsNomarl) {
            try {
                mqttAndroidClient!!.connect(mMqttConnectOptions, null, iMqttActionListener)
                Log.d(TAG, "Connected to MQTT server.")
            } catch (me: MqttException) {
                Log.d(TAG, "Connection failed: ")
            }
        }
    }

    private val isConnectIsNomarl: Boolean
        /**
         * 判断网络是否连接
         */
        get() {
            val connectivityManager =
                this.applicationContext.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            val info = connectivityManager.activeNetworkInfo
            if (info != null && info.isAvailable) {
                val name = info.typeName
                Log.i(TAG, "当前网络名称：$name")
                return true
            } else {
                Log.i(TAG, "没有可用网络")
                /*没有可用网络的时候，延迟3秒再尝试重连*/
                Handler().postDelayed({ doClientConnection() }, 3000)
                return false
            }
        }

    override fun onDestroy() {
        try {
            mqttAndroidClient!!.disconnect() //断开连接
            Log.d(TAG, "断开 mqtt connect")
        } catch (e: MqttException) {
            e.printStackTrace()
        }
        super.onDestroy()
    }

    companion object {
        const val TAG: String = "nice-code"
        var PUBLISH_TOPIC: String = "publish" //发布主题
        var SUBSCRIVE_TOPIC: String = "subscribe" //发布主题

        const val action: String = "com.olh.hypertool" //广播消息

        private var mqttAndroidClient: MqttAsyncClient? = null

        /**
         * 订阅
         */
        fun MQTT_Subscribe(Subscribe_Topic: String) {
            val retained = false // 是否在服务器保留断开连接后的最后一条消息
            try {
                mqttAndroidClient!!.subscribe(Subscribe_Topic, 0)
                Log.d(TAG, "订阅主题$Subscribe_Topic")
            } catch (e: MqttException) {
                e.printStackTrace()
            }
        }

        /**
         * 取消订阅
         */
        fun MQTT_UnSubscribe(UnSubscribe_Topic: String) {
            val retained = false // 是否在服务器保留断开连接后的最后一条消息
            try {
                mqttAndroidClient!!.unsubscribe(UnSubscribe_Topic)
                Log.d(TAG, "取消订阅主题$UnSubscribe_Topic")
            } catch (e: MqttException) {
                e.printStackTrace()
            }
        }

        /**
         * 发布消息
         */
        fun MQTT_Publish(Publish_Topic: String?, qos: Int?, message: String) {
            val retained = false // 是否在服务器保留断开连接后的最后一条消息
            try {
                //参数分别为：主题、消息的字节数组、服务质量、是否在服务器保留断开连接后的最后一条消息
                mqttAndroidClient!!.publish(
                    Publish_Topic, message.toByteArray(),
                    qos!!, retained
                )
            } catch (e: MqttException) {
                e.printStackTrace()
            }
        }
    }

}