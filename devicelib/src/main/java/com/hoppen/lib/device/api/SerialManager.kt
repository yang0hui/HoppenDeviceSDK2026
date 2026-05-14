package com.hoppen.lib.device.api

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Created by CoderHui on 2026/5/12.
 */

class SerialManager private constructor(val appCompatActivity: AppCompatActivity,
                                        val serialConfig: SerialConfig,
                                        val serialListener: SerialListener) : LifecycleEventObserver {
    private val serialClient = SerialClient()

    init {
        appCompatActivity.lifecycle.addObserver(this)
    }

    companion object {
        private var instance: SerialManager? = null

        fun getController(appCompatActivity: AppCompatActivity,
                          serialConfig: SerialConfig,
                          serialListener: SerialListener): SerialController{
            if (instance == null || instance?.appCompatActivity != appCompatActivity) {
                instance = SerialManager(appCompatActivity, serialConfig, serialListener)
            }
            return instance!!.serialClient.controller
        }
    }

    override fun onStateChanged(
        source: LifecycleOwner,
        event: Lifecycle.Event,
    ) {
        when(event){
            Lifecycle.Event.ON_CREATE -> serialClient.goConnect(appCompatActivity,serialConfig,serialListener)
            Lifecycle.Event.ON_START -> serialClient.onStart()
//            Lifecycle.Event.ON_RESUME -> TODO()
//            Lifecycle.Event.ON_PAUSE -> TODO()
            Lifecycle.Event.ON_STOP -> serialClient.onStop()
            Lifecycle.Event.ON_DESTROY -> {
                serialClient.close()
                instance = null
            }
//            Lifecycle.Event.ON_ANY -> TODO()
            else -> {}
        }
    }
}