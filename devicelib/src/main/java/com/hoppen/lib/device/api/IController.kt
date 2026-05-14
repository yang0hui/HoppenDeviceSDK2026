package com.hoppen.lib.device.api

/**
 * Created by CoderHui on 2026/5/12.
 */

interface IController {

    fun closeController()

    fun selectHandle(typeCode: String)

    fun setHandleStart(mode: Int, strength: Int, time: Int)

    fun setHandleStop(mode: Int, strength: Int, time: Int)

    fun setHandleConfig(mode: Int, strength: Int, time: Int)

    fun enterHandleRate()

    fun exitHandleRate()

    fun setHandleRate(data: Int)

    fun getDeviceCode()

    fun getUsbVerInfo()

    fun customInstruction(instruction: ByteArray)

    fun cleanGunOut(sw: Boolean)

    fun cleanGunRinse(sw: Boolean)

}