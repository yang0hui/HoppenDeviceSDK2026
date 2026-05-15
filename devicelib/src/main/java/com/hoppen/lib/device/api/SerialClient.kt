package com.hoppen.lib.device.api

import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.LogUtils
import com.hoppen.lib.device.connection.AbsConnection
import com.hoppen.lib.device.connection.ConnectionFactory
import com.hoppen.lib.device.connection.UsbConnection

/**
 * Created by CoderHui on 2026/5/12.
 */

class SerialClient {

    private var connection: AbsConnection? = null

    val controller: SerialController = SerialController(
        onSend = {data-> send(data)},
        onClose = {close()}
    )

    fun goConnect(appCompatActivity: AppCompatActivity, config: SerialConfig, serialListener: SerialListener) {
        try {

            connection = ConnectionFactory.createConnection(appCompatActivity,config,MainThreadSerialListener(serialListener))

            connection?.open()
        }catch (e: Exception){
            LogUtils.e(e.toString())
        }
    }

    fun close() {
        connection?.close()
    }

    fun onStart(){
        connection?.let {
            if (it is UsbConnection){
                it.onStart()
            }
        }
    }

    fun onStop(){
        connection?.let {
            if (it is UsbConnection){
                it.onStop()
            }
        }
    }

    private fun send(data: ByteArray) {
        connection?.write(data)
    }

}

