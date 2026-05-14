package com.hoppen.lib.device.connection

import com.hoppen.lib.device.api.SerialConfig
import com.hoppen.lib.device.api.SerialListener

/**
 * Created by CoderHui on 2026/5/12.
 */

abstract class AbsConnection {

    @Throws(Exception::class)
    abstract fun open()

    abstract fun close()

    @Throws(Exception::class)
    abstract fun write(data: ByteArray)

}