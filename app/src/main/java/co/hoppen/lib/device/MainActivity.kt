package co.hoppen.lib.device

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android_serialport_api.SerialPortFinder
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import com.blankj.utilcode.util.LogUtils
import com.hoppen.lib.device.api.SerialConfig
import com.hoppen.lib.device.api.SerialController
import com.hoppen.lib.device.api.SerialListener
import com.hoppen.lib.device.api.SerialManager
import com.hoppen.lib.device.api.SerialType
import com.hoppen.lib.device.uart.SerialHelper
import com.hoppen.lib.device.uart.bean.ComBean

class MainActivity : AppCompatActivity(), SerialListener {

    private lateinit var controller: SerialController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        controller = SerialManager.getController(this,
            SerialConfig(SerialType.UART,9600,"/dev/ttyS1"),
            this)

        SerialPortFinder().run {
//            this.allDevices.forEach {
//                LogUtils.e(it)
//            }
            LogUtils.e(this.allDevicesPath)
        }

    }

    override fun onConnected() {
        send("设备已连接")
    }

    override fun onDisconnected() {
        send("设备已断开")
    }

    override fun onReceive(data: String) {
        send(data)
    }

    private fun send(send: String) {
        findViewById<AppCompatTextView>(R.id.tv_callback).run {
            this.text = send + "\n" + this.getText().toString()
        }
        findViewById<ScrollView>(R.id.scrollview).fullScroll(View.FOCUS_UP)
    }

    private fun add(num: Int): Int {
        return num + 1
    }

    private fun sub(num: Int): Int {
        if (num == 0) return num
        return num - 1
    }

    fun time(view: View) {
        val tv_time = findViewById<TextView?>(R.id.tv_time)
        var time = tv_time.getText().toString().toInt()
        when (view.getId()) {
            R.id.btn_time_add -> time = add(time)
            R.id.btn_time_sub -> time = sub(time)
        }
        tv_time.setText(time.toString() + "")
    }

    fun strength(view: View) {
        val tv_strength = findViewById<TextView?>(R.id.tv_strength)
        var strength = tv_strength.getText().toString().toInt()
        when (view.getId()) {
            R.id.btn_strength_add -> strength = add(strength)
            R.id.btn_strength_sub -> strength = sub(strength)
        }
        tv_strength.setText(strength.toString() + "")
    }

    fun mode(view: View) {
        val tv_mode = findViewById<TextView?>(R.id.tv_mode)
        var mode = tv_mode.getText().toString().toInt()
        when (view.getId()) {
            R.id.btn_mode_add -> mode = add(mode)
            R.id.btn_mode_sub -> mode = sub(mode)
        }
        tv_mode.setText(mode.toString() + "")
    }

    fun select(view: View?) {
        val et_code = findViewById<EditText?>(R.id.et_code)
        if (controller != null) controller.selectHandle(et_code.getText().toString())
    }

    fun function(view: View) {
        if (controller == null) return
        val tv_time = findViewById<TextView?>(R.id.tv_time)
        val tv_strength = findViewById<TextView?>(R.id.tv_strength)
        val tv_mode = findViewById<TextView?>(R.id.tv_mode)
        val time = tv_time.getText().toString().toInt() * 60
        val strength = tv_strength.getText().toString().toInt()
        val mode = tv_mode.getText().toString().toInt()
        when (view.getId()) {
            R.id.btn_set -> controller.setHandleConfig(mode, strength, time)
            R.id.btn_start -> controller.setHandleStart(mode, strength, time)
            R.id.btn_stop -> controller.setHandleStop(mode, strength, time)
        }
    }

    fun enter(view: View?) {
        if (controller == null) return
        controller.enterHandleRate()
    }

    fun exit(view: View?) {
        if (controller == null) return
        controller.exitHandleRate()
    }

    fun setRate(view: View?) {
        try {
            if (controller == null) return
            val et_rate = findViewById<EditText?>(R.id.et_rate)
            val rate = et_rate.getText().toString().toInt()
            controller.setHandleRate(rate)
        } catch (e: Exception) {
        }
    }

    fun wskt001(view: View?) {
        //if (controller!=null)controller.getDeviceCode();
        if (controller != null) {
            controller.getDeviceCode()
            //controller.customInstruction(new byte[]{1});
            //controller.cleanGunOut(true);
            //controller.test("/storage/emulated/0/.wax/WaxHairFunctionMachine/apk/护理系统1.2.0.apk")
        }
    }


    fun wskt006(view: View?) {
        if (controller != null) {
            controller.selectHandle("WSKT006");
//            controller.cleanGunRinse(true)
        }
    }

    fun wskt010(view: View?) {
        if (controller != null) {
//            controller.selectHandle("WSKT010");
//            controller.cleanGunRinse(false)
            controller.getDeviceCode()
        }
    }

    fun wskt002(view: View?) {
        if (controller != null) {
//            controller.getUsbVerInfo();
            controller.getDeviceCode()
        }
    }

}