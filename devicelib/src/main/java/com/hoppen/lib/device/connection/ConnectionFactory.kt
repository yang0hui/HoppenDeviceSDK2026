package com.hoppen.lib.device.connection

import androidx.appcompat.app.AppCompatActivity
import com.hoppen.lib.device.api.SerialConfig
import com.hoppen.lib.device.api.SerialListener
import com.hoppen.lib.device.api.SerialType

/**
 * Created by CoderHui on 2026/5/12.
 */

object ConnectionFactory {

    fun createConnection(appCompatActivity: AppCompatActivity, serialConfig: SerialConfig,serialListener: SerialListener): AbsConnection {

        return when(serialConfig.type){
            SerialType.AUTO -> {
                if (serialConfig.serialPath == null) UsbConnection(appCompatActivity,serialListener) else UartConnection(serialConfig,serialListener)
            }
            SerialType.USB -> {
                UsbConnection(appCompatActivity,serialListener)
            }
            SerialType.UART -> {
                if (serialConfig.serialPath == null){
                    throw IllegalArgumentException("serialPath is null")
                }
                UartConnection(serialConfig,serialListener)
            }
        }
    }
}