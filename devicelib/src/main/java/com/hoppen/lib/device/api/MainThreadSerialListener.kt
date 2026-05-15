package com.hoppen.lib.device.api

import android.os.Handler
import android.os.Looper

/**
 * Created by CoderHui on 2026/5/15.
 */

class MainThreadSerialListener(private val original: SerialListener) : SerialListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    override fun onConnected() {
        mainHandler.post {
            original.onConnected()
        }
    }

    override fun onDisconnected() {
        mainHandler.post {
            original.onDisconnected()
        }
    }

    override fun onReceive(data: String) {
        mainHandler.post {
            original.onReceive(data)
        }
    }
}