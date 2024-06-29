package com.olh.mqtttest

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    companion object {
        private const val ACTION = "com.olh.hypertool"
    }

    private val TAG = "nice-code"
    private lateinit var mIntent: Intent
    private val MainActivity_broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "com.olh.hypertool") { // 检查action是否匹配
                if (intent.hasExtra("MQTT_RevMsg")) {
                    val message = intent.getStringExtra("MQTT_RevMsg")
                    Log.d(TAG, "Received message: $message")
                    // 在这里处理接收到的消息，例如更新UI
                    val receive_msg : TextView = findViewById(R.id.textView3)
                    receive_msg.setText(message)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val edit_sub_topic : EditText = findViewById(R.id.edit_subscribe_topic)
        val edit_pub_topic : EditText = findViewById(R.id.edit_publish_topic)
        val edit_msg : EditText = findViewById(R.id.edit_msg)

        val btn_connect:Button = findViewById(R.id.btn_cet)
        val btn_sent:Button = findViewById(R.id.btn_send)

        /************************* 开启服务 *************************/
        mIntent = Intent(this, MyMqttService::class.java)
        startService(mIntent)
        /************************* 开启服务 *************************/

        /************************* 通过广播接收的方式从MqttService中接收数据 *************************/
        // 在MainActivity中注册BroadcastReceiver
        val filter = IntentFilter("com.olh.hypertool")
        registerReceiver(MainActivity_broadcastReceiver, filter, Context.RECEIVER_EXPORTED)




        btn_connect.setOnClickListener {
            val text_topic : String = edit_sub_topic.text.toString()
            Log.d(TAG,"主题: ${text_topic}")
            MyMqttService.MQTT_Subscribe(text_topic)
        }

        btn_sent.setOnClickListener {
            val text_pub_topic = edit_pub_topic.text.toString()
            val text_msg = edit_msg.text.toString()
            // 设置发布主题和消息
            MyMqttService.MQTT_Publish(text_pub_topic,0,text_msg)
        }




    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(MainActivity_broadcastReceiver) // 记得取消注册广播接收器
    }
}