package co.hoppen.lib.device

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android_serialport_api.SerialPortFinder
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.Utils
import com.hoppen.lib.device.uart.SerialHelper
import com.hoppen.lib.device.uart.bean.ComBean

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        SerialPortFinder().run {
            this.allDevices.forEach {
                LogUtils.e(it)
            }
            this.allDevicesPath.forEach {
                LogUtils.e(it)
            }
        }

        val a = object : SerialHelper("/dev/ttyS3", 9600) {
            override fun onDataReceived(comBean: ComBean) {
                comBean.let {
                    LogUtils.d("收到数据: ${String(it.bRec)}")
                }
            }
        }
        a.open()


    }




}