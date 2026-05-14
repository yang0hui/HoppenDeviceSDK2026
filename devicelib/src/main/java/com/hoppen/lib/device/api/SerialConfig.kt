package com.hoppen.lib.device.api

/**
 * Created by CoderHui on 2026/5/12.
 */

data class SerialConfig(val type: SerialType,
                        val baudRate: Int = 9600,
                        val serialPath: String? = null,){

}


enum class SerialType {
    AUTO,
    USB,
    UART
}