package com.hoppen.lib.device.api

/**
 * Created by CoderHui on 2026/5/12.
 */

interface SerialListener {
    fun onConnected()

    fun onDisconnected()

    fun onReceive(data: String)

}

