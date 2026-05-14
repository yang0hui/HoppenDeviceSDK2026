package com.hoppen.lib.device.usb

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ThreadUtils
import com.blankj.utilcode.util.ThreadUtils.SimpleTask
import com.hoppen.lib.device.api.SerialListener
import com.hoppen.lib.device.command.Command
import com.hoppen.lib.device.usb.queue.ConnectMcuDeviceTask
import com.hoppen.lib.device.utils.CommandUtils
import java.util.concurrent.TimeUnit


/**
 * Created by CoderHui on 2026/5/13.
 */

class McuDevice(val serialListener: SerialListener) {
    private var usbDeviceConnection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null
    private var epOut: UsbEndpoint? = null
    private var epIn: UsbEndpoint? = null

    private val DEFAULT_MAX_READ_BYTES = 128
    private val DEFAULT_TIMEOUT = 200
    private var readDataThread: Thread? = null

    private val readRunnable = Runnable {
        while (usbDeviceConnection != null) {
            try {
                val bytes = readData()
                if (bytes != null) {
                    val strings = CommandUtils.decodingData(bytes)
                    if (strings != null) {
                        for (data in strings) {
                            if (data != "System-OnLine") {
                                serialListener.onReceive(data)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    var loopSend: ThreadUtils.Task<*> = object : SimpleTask<ByteArray>() {
        override fun doInBackground(): ByteArray {
            return Command.USB_SYS_ONLINE()
        }

        override fun onSuccess(data: ByteArray) {
            asySendInstructions(data)
        }
    }

    @Synchronized
    fun onConnecting(connectMcuInfo: ConnectMcuDeviceTask.ConnectMcuInfo) {
        usbDeviceConnection = connectMcuInfo.usbDeviceConnection
        usbInterface = connectMcuInfo.usbInterface
        epOut = connectMcuInfo.epOut
        epIn = connectMcuInfo.epIn

        serialListener.onConnected()

        ThreadUtils.executeBySingleAtFixRate(loopSend,5, 5,TimeUnit.SECONDS)

        readDataThread = Thread(readRunnable)
        readDataThread?.start()
    }

    fun onDisconnect(usbDevice: UsbDevice, type: DeviceType) {
        serialListener.onDisconnected()
        closeDevice()
    }

    fun asySendInstructions(bytes: ByteArray) {
        asySendInstructions(bytes, DEFAULT_TIMEOUT)
    }

    fun closeDevice() {
        usbDeviceConnection?.let { connection ->
            try {
                loopSend.cancel()

                if (readDataThread != null && readDataThread!!.isAlive) {
                    readDataThread?.interrupt()
                    readDataThread = null
                }

                LogUtils.e(usbInterface == null, usbInterface.toString())

                usbInterface?.let {
                    connection.releaseInterface(it)
                }
                connection.close()

                usbDeviceConnection = null
                usbInterface = null
                epOut = null
                epIn = null

                LogUtils.e("USB device closed")
            } catch (e: Exception) {
                LogUtils.e("Error closing USB device", e)
            }
        }
    }
    private fun asySendInstructions(data: ByteArray, timeOut: Int) {
        ThreadUtils.executeBySingle(object : SimpleTask<Boolean>() {
            override fun doInBackground(): Boolean {
                if (usbDeviceConnection != null && epOut != null) {
                    val i = usbDeviceConnection!!.bulkTransfer(epOut, data, data.size, timeOut)
                    return i > 0
                }else return false
            }
            override fun onSuccess(p0: Boolean) {
                LogUtils.e("发送${p0}")
            }
        })
    }

    private fun readData(): ByteArray? {
        val data = ByteArray(DEFAULT_MAX_READ_BYTES)
        val cnt = usbDeviceConnection?.bulkTransfer(epIn, data, data.size, DEFAULT_TIMEOUT) ?: -1
        return if (cnt != -1) {
            data.copyOfRange(0, cnt)
        } else {
            null
        }
    }

}