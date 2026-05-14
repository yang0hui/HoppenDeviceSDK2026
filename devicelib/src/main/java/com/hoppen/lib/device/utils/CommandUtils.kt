package com.hoppen.lib.device.utils

/**
 * Created by CoderHui on 2026/5/13.
 */

object CommandUtils {

    fun decodingData(data: ByteArray): Array<String>? {
        var decodeData: Array<String>? = null
        try {
            val stringData = String(data)
            val split =
                stringData.split("]>".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (i in split.indices) {
                split[i] = split[i].replace("<[", "")
            }
            decodeData = split
        } catch (e: Exception) {
        }
        return decodeData
    }

}