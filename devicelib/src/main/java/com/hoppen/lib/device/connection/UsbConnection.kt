package com.hoppen.lib.device.connection

import android.content.Context
import android.hardware.usb.UsbDevice
import androidx.appcompat.app.AppCompatActivity
import com.hoppen.lib.device.api.SerialConfig
import com.hoppen.lib.device.api.SerialListener
import com.hoppen.lib.device.usb.DeviceType
import com.hoppen.lib.device.usb.McuDevice
import com.hoppen.lib.device.usb.OnUsbStatusListener
import com.hoppen.lib.device.usb.UsbMonitor
import com.hoppen.lib.device.usb.queue.ConnectMcuDeviceTask
import com.hoppen.lib.device.usb.queue.TaskQueue

/**
 * Created by CoderHui on 2026/5/12.
 */

class UsbConnection(private val appCompatActivity: AppCompatActivity,serialListener: SerialListener) : AbsConnection(),
    OnUsbStatusListener {

    private val usbMonitor = UsbMonitor(this)

    private val mcuDevice = McuDevice(serialListener)

    private val taskQueue: TaskQueue = TaskQueue()

    override fun open() {
        usbMonitor.requestDeviceList(appCompatActivity)
    }

    override fun close() {
        mcuDevice.closeDevice()
    }

    override fun write(data: ByteArray) {
        mcuDevice.asySendInstructions(data)
    }


    fun onStart(){
        usbMonitor.register(appCompatActivity)
    }

    fun onStop(){
        usbMonitor.unregister(appCompatActivity)
    }

    override fun onConnecting(
        usbDevice: UsbDevice,
        type: DeviceType,
    ) {
        if (type == DeviceType.MCU) {
            //mcuDevice.onConnecting(usbDevice, type)
            val connectMcuDeviceTask = ConnectMcuDeviceTask(usbDevice)
            taskQueue.addTask(connectMcuDeviceTask) {
                val connectMcuInfo = connectMcuDeviceTask.connectMcuInfo
                if (connectMcuInfo.isConform) {
                    mcuDevice.onConnecting(connectMcuInfo)
                }
            }
        }
    }

    override fun onDisconnect(
        usbDevice: UsbDevice,
        type: DeviceType,
    ) {
        if (type == DeviceType.MCU) {
            mcuDevice.onDisconnect(usbDevice, type)
        }
    }

}