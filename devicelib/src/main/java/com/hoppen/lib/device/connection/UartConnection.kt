package com.hoppen.lib.device.connection

import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ThreadUtils
import com.blankj.utilcode.util.ThreadUtils.SimpleTask
import com.hoppen.lib.device.api.SerialConfig
import com.hoppen.lib.device.api.SerialListener
import com.hoppen.lib.device.command.Command
import com.hoppen.lib.device.uart.SerialHelper
import com.hoppen.lib.device.uart.bean.ComBean
import com.hoppen.lib.device.utils.CommandUtils
import java.util.concurrent.TimeUnit

/**
 * Created by CoderHui on 2026/5/12.
 */

class UartConnection(private val serialConfig: SerialConfig,val serialListener: SerialListener) : AbsConnection() {

    private var serialHelper: SerialHelper? = null

    private var connectStatus = false

    private var connected = false

    private var lastOnlineTime: Long = 0

    private var loopSend: ThreadUtils.Task<*> = object : SimpleTask<ByteArray>() {
        override fun doInBackground(): ByteArray {
            return Command.USB_SYS_ONLINE()
        }

        override fun onSuccess(data: ByteArray) {
            write(data)
        }
    }

    private var loopCheckStatus: ThreadUtils.Task<*> = object : SimpleTask<Boolean>() {
        override fun doInBackground(): Boolean {
            val elapsed = System.currentTimeMillis() - lastOnlineTime
            return elapsed >= 6000
        }

        override fun onSuccess(timeout: Boolean) {
            if (timeout){
                if (connected){
                    if (connectStatus)serialListener.onDisconnected()
                    connectStatus = false
                    connected = false
                }
            }
        }
    }


    override fun open() {

        try {
            serialHelper = object : SerialHelper(serialConfig.serialPath!!, serialConfig.baudRate) {
                override fun onDataReceived(comBean: ComBean) {
                    CommandUtils.decodingData(comBean.bRec)?.let {strings->
                        for (data in strings) {
                            LogUtils.e(data)
                            if (data != "System-OnLine") {
                                serialListener.onReceive(data)
                            }else {
                                lastOnlineTime = System.currentTimeMillis()
                                if (!connectStatus)serialListener.onConnected()
                                connectStatus = true
                                connected = true
                            }
                        }
                    }
                }
            }
            LogUtils.e(serialHelper?.toString())
            serialHelper?.open()

            lastOnlineTime = System.currentTimeMillis()

            ThreadUtils.executeBySingleAtFixRate(loopSend,2, 5,TimeUnit.SECONDS)

            ThreadUtils.executeBySingleAtFixRate(loopCheckStatus,1, 1,TimeUnit.SECONDS)

        }catch (e : Exception){
            LogUtils.e(e.toString())
        }
    }

    override fun close() {
        loopSend.cancel()
        loopCheckStatus.cancel()

        serialHelper?.close()
        serialHelper = null
    }

    override fun write(data: ByteArray) {
//        LogUtils.e(data)
        serialHelper?.send(data)
    }
}