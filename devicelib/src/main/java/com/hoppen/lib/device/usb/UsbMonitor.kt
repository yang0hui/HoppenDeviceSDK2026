package com.hoppen.lib.device.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.Utils

/**
 * Created by CoderHui on 2026/5/13.
 */

class UsbMonitor(
    private val onUsbStatusListener: OnUsbStatusListener,
) {
    private val usbManager: UsbManager by lazy {
        Utils.getApp().getSystemService(Context.USB_SERVICE) as UsbManager
    }
    private val USB_PERMISSION = UsbMonitor::class.java.name
    private var usbReceiver: BroadcastReceiver? = null
    private val filterList: List<DeviceFilter> = DeviceFilter.getDeviceFilters()
    private val doubleCheckMap: MutableMap<String, UsbDevice> = HashMap()

    fun register(context: Context) {
        initUsbReceiver()
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(USB_PERMISSION)
        }
        context.registerReceiver(usbReceiver, filter)
    }

    private fun initUsbReceiver() {
        if (usbReceiver == null) {
            usbReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val action = intent.action
                    if (action == UsbManager.ACTION_USB_DEVICE_ATTACHED ||
                        action == UsbManager.ACTION_USB_DEVICE_DETACHED ||
                        action == USB_PERMISSION
                    ) {
                        @Suppress("DEPRECATION")
                        val usbDevice = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                        if (usbDevice != null) {
                            LogUtils.e(usbDevice.vendorId, usbDevice.productId)
                            val hoppenDevice = deviceFilter(usbDevice)
                            if (hoppenDevice != null) {
                                when (action) {
                                    UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                                        doubleCheckMap[usbDevice.deviceName] = usbDevice
                                        requestPermission(context, usbDevice)
                                    }
                                    UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                                        onUsbStatusListener.onDisconnect(usbDevice, hoppenDevice.type)
                                    }
                                    USB_PERMISSION -> {
                                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                                            doubleCheckMap.remove(usbDevice.deviceName)
                                            onUsbStatusListener.onConnecting(usbDevice, hoppenDevice.type)
                                            doubleCheckPermission(context)
                                        } else {
                                            requestPermission(context, usbDevice)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun doubleCheckPermission(context: Context) {
        for (value in doubleCheckMap.values) {
            requestPermission(context, value)
        }
    }

    fun unregister(context: Context) {
        usbReceiver?.let {
            context.unregisterReceiver(it)
            usbReceiver = null
        }
    }

    private fun requestPermission(context: Context, usbDevice: UsbDevice?): Boolean {
        if (usbDevice != null) {
            if (!usbManager.hasPermission(usbDevice)) {
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    Intent(USB_PERMISSION),
                    PendingIntent.FLAG_IMMUTABLE
                )
                usbManager.requestPermission(usbDevice, pendingIntent)
                return true
            } else {
                doubleCheckMap.remove(usbDevice.deviceName)
            }
        }
        return false
    }

    fun requestDeviceList(context: Context): List<UsbDevice> {
        val usbDevices = ArrayList(usbManager.deviceList.values)
        val iterator = usbDevices.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            LogUtils.e("${next.vendorId}   ${next.productId}")
            val hoppenDevice = deviceFilter(next)
            if (hoppenDevice != null) {
                doubleCheckMap[next.deviceName] = next
                val need = requestPermission(context, next)
                if (!need) onUsbStatusListener.onConnecting(next, hoppenDevice.type)
            }
        }
        return usbDevices
    }

    private fun deviceFilter(usbDevice: UsbDevice): DeviceFilter? {
        if (filterList.isEmpty()) return null
        for (deviceFilter in filterList) {
            if (deviceFilter.mProductId == usbDevice.productId &&
                deviceFilter.mVendorId == usbDevice.vendorId
            ) {
                return deviceFilter
            }
        }
        return null
    }
}