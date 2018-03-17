package cn.thinkbear.app.running.interf;

import java.util.UUID;

/**
 * 使用时替换下列为你对应的uuid
 * Created by qin on 2017/1/31.
 */

public interface BluetoothUUID {
    //电池电量
    UUID BATTERY_SERVICE = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
    UUID BATTERY_NOTIFICATION = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");

//    UUID serviceUUID = UUID.fromString("FFE0");
//    UUID txCharacteristic = UUID.fromString("FFE1");
//    UUID rxCharacteristic = UUID.fromString("FFE1");

    UUID psServiceUUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    UUID psReadUUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
    UUID psWriteUUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
    UUID PS_STEP_NOTIFICATION = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
    UUID PS_HR_NOTIFICATION = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");


}
