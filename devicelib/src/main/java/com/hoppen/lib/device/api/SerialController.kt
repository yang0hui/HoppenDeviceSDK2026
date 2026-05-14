package com.hoppen.lib.device.api

import com.hoppen.lib.device.command.Command

/**
 * Created by CoderHui on 2026/5/12.
 */

class SerialController(val onSend: (ByteArray) -> Unit, val onClose:()->Unit):IController{
    override fun closeController() {
        onClose()
    }
    override fun selectHandle(typeCode: String) {
        onSend(Command.USB_HANDLE_SET(typeCode))
    }

    override fun setHandleStart(mode: Int, strength: Int, time: Int) {
        onSend(Command.USB_HANDLE_START(mode, strength, time))
    }

    override fun setHandleStop(mode: Int, strength: Int, time: Int) {
        onSend(Command.USB_HANDLE_STOP(mode, strength, time))
    }

    override fun setHandleConfig(mode: Int, strength: Int, time: Int) {
        onSend(Command.USB_HANDLE_CONFIG(mode, strength, time))
    }

    override fun enterHandleRate() {
        onSend(Command.USB_ENTER_RATE())
    }

    override fun exitHandleRate() {
        onSend(Command.USB_EXIT_RATE())
    }

    override fun setHandleRate(data: Int) {
        onSend(Command.USB_SET_RATE(data))
    }

    override fun getDeviceCode() {
        onSend(Command.USB_DEVICE_CODE())
    }

    override fun getUsbVerInfo() {
        onSend(Command.USB_VER_INFO())
    }

    override fun customInstruction(instruction: ByteArray) {
        onSend(instruction)
    }

    override fun cleanGunOut(sw: Boolean) {
        onSend(Command.USB_CLEAN_GUN_OUT(sw))
    }

    override fun cleanGunRinse(sw: Boolean) {
        onSend(Command.USB_CLEAN_GUN_RINSE(sw))
    }
}