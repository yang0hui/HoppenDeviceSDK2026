package com.hoppen.lib.device.usb.queue;

import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.Utils;
import com.hoppen.lib.device.command.Command;

import java.util.Arrays;

/**
 * Created by YangJianHui on 2022/5/4.
 */
public class ConnectMcuDeviceTask extends Task{
   private ConnectMcuInfo connectMcuInfo;

   public ConnectMcuDeviceTask(UsbDevice usbDevice){
      connectMcuInfo = new ConnectMcuInfo((UsbManager) Utils.getApp().getSystemService(Context.USB_SERVICE),usbDevice);
   }

   public ConnectMcuInfo getConnectMcuInfo() {
      return connectMcuInfo;
   }

   @Override
   public void taskContent() {
      if (connectMcuInfo!=null){
         UsbManager usbManager = connectMcuInfo.usbManager;
         UsbDevice usbDevice = connectMcuInfo.usbDevice;
         UsbDeviceConnection usbDeviceConnection = usbManager.openDevice(usbDevice);
         int interfaceCount = usbDevice.getInterfaceCount();
         LogUtils.e("interface count :"+interfaceCount,usbDevice.toString());
         if (usbDeviceConnection!=null && interfaceCount>0) {
            UsbInterface usbInterface;
            UsbEndpoint epOut = null,epIn = null;

            usbInterface = usbDevice.getInterface(interfaceCount - 1);

            boolean claimInterface = usbDeviceConnection.claimInterface(usbInterface, true);
            LogUtils.e("claimInterface:"+claimInterface);
            if (claimInterface) {
               //设置波特率等设置
               setConfig(usbDeviceConnection, 9600, 8, 1, 0);
//               setConfig(usbDeviceConnection,9600, (byte) 8, (byte) 1, (byte) 0, (byte) 0);
               for (int index = 0; index < usbInterface.getEndpointCount(); index++) {
                  UsbEndpoint ep = usbInterface.getEndpoint(index);
                  if ((ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK)
                          && (ep.getDirection() == UsbConstants.USB_DIR_OUT)) {
                     epOut = ep;
                  }
                  if ((ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK)
                          && (ep.getDirection() == UsbConstants.USB_DIR_IN)) {
                     epIn = ep;
                  }
               }
               connectMcuInfo.usbDeviceConnection = usbDeviceConnection;
               connectMcuInfo.usbInterface = usbInterface;
               connectMcuInfo.epOut = epOut;
               connectMcuInfo.epIn = epIn;

               //-----------
               boolean through = discernDevice(Command.USB_DEVICE_CODE(), connectMcuInfo);
               LogUtils.e("through:"+through);
//               through = true;
               connectMcuInfo.conform = through;
               if (!through){
                  try {
                     usbDeviceConnection.releaseInterface(usbInterface);
                     usbDeviceConnection.close();
                     connectMcuInfo.usbDeviceConnection = usbDeviceConnection=null;
                     connectMcuInfo.usbInterface = usbInterface = null;
                     connectMcuInfo.epOut = epOut = null;
                     connectMcuInfo.epIn  = epIn = null;
                  }catch (Exception e){
                  }
               }
            }
         }
      }
   }

   private synchronized boolean sendInstructions(byte [] data , ConnectMcuInfo connectMcuInfo){
      boolean success = false;
      if (connectMcuInfo.usbDeviceConnection!=null&&connectMcuInfo.epOut!=null&&data!=null){
         int i= connectMcuInfo.usbDeviceConnection.bulkTransfer(connectMcuInfo.epOut,data,data.length,300);
         success = i>0;
      }
      return  success;
   }


   private byte[] readData(ConnectMcuInfo connectMcuInfo){
      final byte[] data = new byte[128];
      int cnt = connectMcuInfo.usbDeviceConnection.bulkTransfer(connectMcuInfo.epIn, data, data.length, 300);
      if (cnt!=-1){
         byte[] bytes = Arrays.copyOfRange(data, 0, cnt);
         return bytes;
      }else return null;
   }

   private void setConfig(UsbDeviceConnection connection, int baudRate, int dataBit, int stopBit, int parity) {
      int value = 0;
      int index = 0;
      char valueHigh = 0, valueLow = 0, indexHigh = 0, indexLow = 0;
      switch (parity) {
         case 0: /* NONE */
            valueHigh = 0x00;
            break;
         case 1: /* ODD */
            valueHigh |= 0x08;
            break;
         case 2: /* Even */
            valueHigh |= 0x18;
            break;
         case 3: /* Mark */
            valueHigh |= 0x28;
            break;
         case 4: /* Space */
            valueHigh |= 0x38;
            break;
         default: /* None */
            valueHigh = 0x00;
            break;
      }

      if (stopBit == 2) {
         valueHigh |= 0x04;
      }

      switch (dataBit) {
         case 5:
            valueHigh |= 0x00;
            break;
         case 6:
            valueHigh |= 0x01;
            break;
         case 7:
            valueHigh |= 0x02;
            break;
         case 8:
            valueHigh |= 0x03;
            break;
         default:
            valueHigh |= 0x03;
            break;
      }

      valueHigh |= 0xc0;
      valueLow = 0x9c;

      value |= valueLow;
      value |= (int) (valueHigh << 8);

      switch (baudRate) {
         case 50:
            indexLow = 0;
            indexHigh = 0x16;
            break;
         case 75:
            indexLow = 0;
            indexHigh = 0x64;
            break;
         case 110:
            indexLow = 0;
            indexHigh = 0x96;
            break;
         case 135:
            indexLow = 0;
            indexHigh = 0xa9;
            break;
         case 150:
            indexLow = 0;
            indexHigh = 0xb2;
            break;
         case 300:
            indexLow = 0;
            indexHigh = 0xd9;
            break;
         case 600:
            indexLow = 1;
            indexHigh = 0x64;
            break;
         case 1200:
            indexLow = 1;
            indexHigh = 0xb2;
            break;
         case 1800:
            indexLow = 1;
            indexHigh = 0xcc;
            break;
         case 2400:
            indexLow = 1;
            indexHigh = 0xd9;
            break;
         case 4800:
            indexLow = 2;
            indexHigh = 0x64;
            break;
         case 9600:
            indexLow = 2;
            indexHigh = 0xb2;
            break;
         case 19200:
            indexLow = 2;
            indexHigh = 0xd9;
            break;
         case 38400:
            indexLow = 3;
            indexHigh = 0x64;
            break;
         case 57600:
            indexLow = 3;
            indexHigh = 0x98;
            break;
         case 115200:
            indexLow = 3;
            indexHigh = 0xcc;
            break;
         case 230400:
            indexLow = 3;
            indexHigh = 0xe6;
            break;
         case 460800:
            indexLow = 3;
            indexHigh = 0xf3;
            break;
         case 500000:
            indexLow = 3;
            indexHigh = 0xf4;
            break;
         case 921600:
            indexLow = 7;
            indexHigh = 0xf3;
            break;
         case 1000000:
            indexLow = 3;
            indexHigh = 0xfa;
            break;
         case 2000000:
            indexLow = 3;
            indexHigh = 0xfd;
            break;
         case 3000000:
            indexLow = 3;
            indexHigh = 0xfe;
            break;
         default: // default baudRate "9600"
            indexLow = 2;
            indexHigh = 0xb2;
            break;
      }

      index |= 0x88 | indexLow;
      index |= (int) (indexHigh << 8);

      connection.controlTransfer(
              (0x02 << 5) | 0x00 | 0x00, 0xA1, value, index, null,
              0, 200);
   }

   public void setConfig(UsbDeviceConnection connection,int baudRate, byte dataBits, byte stopBits, byte parity, byte flowControl) {
      char var10;
      switch (parity) {
         case 0:
            var10 = 0;
            break;
         case 1:
            var10 = '\b';
            break;
         case 2:
            var10 = 24;
            break;
         case 3:
            var10 = '(';
            break;
         case 4:
            var10 = '8';
            break;
         default:
            var10 = 0;
      }

      if (stopBits == 2) {
         var10 = (char) (var10 | 4);
      }

      switch (dataBits) {
         case 5:
            var10 = var10;
            break;
         case 6:
            var10 = (char) (var10 | 1);
            break;
         case 7:
            var10 = (char) (var10 | 2);
            break;
         case 8:
            var10 = (char) (var10 | 3);
            break;
         default:
            var10 = (char) (var10 | 3);
      }

      var10 = (char) (var10 | 192);
      int var7 = 156 | var10 << 8;
      byte var6;
      short var9;
      switch (baudRate) {
         case 50:
            var6 = 0;
            var9 = 22;
            break;
         case 75:
            var6 = 0;
            var9 = 100;
            break;
         case 110:
            var6 = 0;
            var9 = 150;
            break;
         case 135:
            var6 = 0;
            var9 = 169;
            break;
         case 150:
            var6 = 0;
            var9 = 178;
            break;
         case 300:
            var6 = 0;
            var9 = 217;
            break;
         case 600:
            var6 = 1;
            var9 = 100;
            break;
         case 1200:
            var6 = 1;
            var9 = 178;
            break;
         case 1800:
            var6 = 1;
            var9 = 204;
            break;
         case 2400:
            var6 = 1;
            var9 = 217;
            break;
         case 4800:
            var6 = 2;
            var9 = 100;
            break;
         case 9600:
            var6 = 2;
            var9 = 178;
            break;
         case 19200:
            var6 = 2;
            var9 = 217;
            break;
         case 38400:
            var6 = 3;
            var9 = 100;
            break;
         case 57600:
            var6 = 3;
            var9 = 152;
            break;
         case 115200:
            var6 = 3;
            var9 = 204;
            break;
         case 230400:
            var6 = 3;
            var9 = 230;
            break;
         case 460800:
            var6 = 3;
            var9 = 243;
            break;
         case 500000:
            var6 = 3;
            var9 = 244;
            break;
         case 921600:
            var6 = 7;
            var9 = 243;
            break;
         case 1000000:
            var6 = 3;
            var9 = 250;
            break;
         case 2000000:
            var6 = 3;
            var9 = 253;
            break;
         case 3000000:
            var6 = 3;
            var9 = 254;
            break;
         default:
            var6 = 2;
            var9 = 178;
      }
      baudRate = 136 | var6 | var9 << 8;
      //161,var7
      baudRate = connection.controlTransfer(64,161,var7,baudRate,null,0,500);
//      if (flowControl == 1) {
//         byte var11 = 96;
//         connection.controlTransfer(164, ~var11, 0);
//      }
      LogUtils.e(baudRate);
   }


   //测试暂时通过
   private boolean discernDevice(byte [] data,ConnectMcuInfo connectMcuInfo){
      try {
         boolean success = sendInstructions(data,connectMcuInfo);
         if (success){
            byte[] bytes = readData(connectMcuInfo);
            if (bytes!=null){
               String device="";
               try {
                  device = new String(bytes);
                  device = device.substring(device.indexOf("<[") + 2, device.lastIndexOf("]>")).trim();
                  LogUtils.e(device);
               }catch (Exception e){
               }
               if (device.contains("W003-8888-NURT-")
                       || device.contains("W003-0003-NURT-"))return true;
            }
         }
      }catch (Exception e){
      }
      ///*************************
      return false;
   }

   public class ConnectMcuInfo{
      public UsbDevice usbDevice;
      private UsbManager usbManager;
      private UsbInterface usbInterface = null;
      private UsbEndpoint epOut = null,epIn = null;
      private UsbDeviceConnection usbDeviceConnection = null;
      private boolean conform = false;

      public ConnectMcuInfo(UsbManager usbManager,UsbDevice usbDevice){
         this.usbManager = usbManager;
         this.usbDevice = usbDevice;
      }

      public UsbInterface getUsbInterface() {
         return usbInterface;
      }

      public UsbEndpoint getEpOut() {
         return epOut;
      }

      public UsbEndpoint getEpIn() {
         return epIn;
      }

      public UsbDeviceConnection getUsbDeviceConnection() {
         return usbDeviceConnection;
      }

      public boolean isConform() {
         return conform;
      }

   }


}
