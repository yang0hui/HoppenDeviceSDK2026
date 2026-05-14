package com.hoppen.lib.device.api

import androidx.appcompat.app.AppCompatActivity
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

        connection = ConnectionFactory.createConnection(appCompatActivity,config,serialListener)

        connection?.open()
    }

    fun close() {
        connection?.close()
    }

    fun onStart(){
        (connection as UsbConnection?)?.onStart()
    }

    fun onStop(){
        (connection as UsbConnection?)?.onStop()
    }

    private fun send(data: ByteArray) {
        connection?.write(data)
    }

}

