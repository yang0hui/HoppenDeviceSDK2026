package com.hoppen.lib.device.command

/**
 * Created by CoderHui on 2026/5/12.
 */

object Command {
    @JvmStatic
    fun USB_DEVICE_CODE(): ByteArray {
        val linePackage = byteArrayOf(
            0xAA.toByte(), 0x01.toByte(), 0x01.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte()
        )
        return encryption(linePackage)
    }

    @JvmStatic
    fun USB_VER_INFO(): ByteArray {
        val bytes = byteArrayOf(
            0xAA.toByte(), 0x01.toByte(), 0x02.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte()
        )
        return encryption(bytes)
    }

    @JvmStatic
    fun USB_SYS_ONLINE(): ByteArray {
        val linePackage = byteArrayOf(
            0xAA.toByte(), 0x01.toByte(), 0x03.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte()
        )
        return encryption(linePackage)
    }

    private fun encryption(data: ByteArray): ByteArray {
        val returnData = ByteArray(data.size + 1)
        try {
            var a: Byte = 0
            for (i in data.indices) {
                returnData[i] = data[i]
                if (i != 0) {
                    a = (a.toInt() xor data[i].toInt()).toByte()
                }
            }
            returnData[data.size] = a
        } catch (e: Exception) {
        }
        return returnData
    }

    @JvmStatic
    fun USB_HANDLE_START(modePosition: Int, strength: Int, time: Int): ByteArray {
        val mode = modePosition.toByte()
        val mStrength = strength.toByte()
        val mtime_1 = (time / 256).toByte()
        val mtime_2 = (time % 256).toByte()
        val config = byteArrayOf(
            0xAA.toByte(), 0x10.toByte(), mode, mStrength, mtime_1, mtime_2
        )
        return encryption(config)
    }

    @JvmStatic
    fun USB_HANDLE_STOP(modePosition: Int, strength: Int, time: Int): ByteArray {
        val mode = modePosition.toByte()
        val mStrength = strength.toByte()
        val mtime_1 = (time / 256).toByte()
        val mtime_2 = (time % 256).toByte()
        val config = byteArrayOf(
            0xAA.toByte(), 0x11.toByte(), mode, mStrength, mtime_1, mtime_2
        )
        return encryption(config)
    }

    @JvmStatic
    fun USB_HANDLE_SET(typeName: String): ByteArray {
        val handle = when (typeName) {
            "WSKT001" -> 0x01.toByte()
            "WSKT002" -> 0x0A.toByte()
            "WSKT003" -> 0x05.toByte()
            "WSKT004" -> 0x03.toByte()
            "WSKT005" -> 0x0F.toByte()
            "WSKT006" -> 0x04.toByte()
            "WSKT007" -> 0x06.toByte()
            "WSKT008" -> 0x07.toByte()
            "WSKT009" -> 0x11.toByte()
            "WSKT010" -> 0x09.toByte()
            "WSKT011" -> 0x0B.toByte()
            "WSKT012" -> 0x10.toByte()
            "WSKT013" -> 0x02.toByte()
            "WSKT014" -> 0x0C.toByte()
            "WSKT015" -> 0x0D.toByte()
            "WSKT016" -> 0x0E.toByte()
            "WSKT017" -> 0x08.toByte()
            "WSKT019" -> 0x12.toByte()
            "WSKT020" -> 0x13.toByte()
            "WSKT021" -> 0x14.toByte()
            "WSKT022" -> 0x15.toByte()
            "WSKT023" -> 0x16.toByte()
            "WSKT024" -> 0x17.toByte()
            "WSKT025" -> 0x19.toByte()
            "WSKT026" -> 0x1A.toByte()
            "WSKT027" -> 0x1B.toByte()
            "WSKT028" -> 0x1C.toByte()
            "WSKT029" -> 0x1D.toByte()
            "WSKT030" -> 0x1E.toByte()
            else -> nameChange(typeName)
        }
        val config = byteArrayOf(
            0xAA.toByte(), 0x40.toByte(), 0x00.toByte(),
            0x00.toByte(), 0x00.toByte(), handle
        )
        return encryption(config)
    }

    @JvmStatic
    fun USB_ENTER_RATE(): ByteArray {
        val config = byteArrayOf(
            0xAA.toByte(), 0x13.toByte(), 0x01.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte()
        )
        return encryption(config)
    }

    @JvmStatic
    fun USB_EXIT_RATE(): ByteArray {
        val config = byteArrayOf(
            0xAA.toByte(), 0x13.toByte(), 0x03.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte()
        )
        return encryption(config)
    }

    @JvmStatic
    fun USB_SET_RATE(data: Int): ByteArray {
        val a = (data shr 8).toByte()
        val b = (data and 0xff).toByte()
        val config = byteArrayOf(
            0xAA.toByte(), 0x13.toByte(), 0x02.toByte(),
            0x00.toByte(), a, b
        )
        return encryption(config)
    }

    @JvmStatic
    fun USB_HANDLE_CONFIG(modePosition: Int, strength: Int, time: Int): ByteArray {
        val mode = modePosition.toByte()
        val mStrength = strength.toByte()
        val mtime_1 = if (time == 0) 0.toByte() else (time / 256).toByte()
        val mtime_2 = if (time == 0) 0.toByte() else (time % 256).toByte()
        val config = byteArrayOf(
            0xAA.toByte(), 0x12.toByte(), mode, mStrength, mtime_1, mtime_2
        )
        return encryption(config)
    }

    @JvmStatic
    fun USB_CLEAN_GUN_OUT(sw: Boolean): ByteArray {
        val config = byteArrayOf(
            0xAA.toByte(), 0x14.toByte(), 0x01.toByte(),
            (if (sw) 0x01 else 0).toByte(), 0.toByte(), 0.toByte()
        )
        return encryption(config)
    }

    @JvmStatic
    fun USB_CLEAN_GUN_RINSE(sw: Boolean): ByteArray {
        val config = byteArrayOf(
            0xAA.toByte(), 0x14.toByte(), 0x02.toByte(),
            (if (sw) 0x01 else 0).toByte(), 0.toByte(), 0.toByte()
        )
        return encryption(config)
    }

    @JvmStatic
    fun USB_FUNCTION_FINISH(): ByteArray {
        val config = byteArrayOf(
            0xAA.toByte(), 0x15.toByte(), 0x01.toByte(),
            0.toByte(), 0.toByte(), 0.toByte()
        )
        return encryption(config)
    }

    private fun nameChange(name: String?): Byte {
        return try {
            if (name != null) {
                val a = name.substring(name.length - 3)
                a.toInt().toByte()
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }

}