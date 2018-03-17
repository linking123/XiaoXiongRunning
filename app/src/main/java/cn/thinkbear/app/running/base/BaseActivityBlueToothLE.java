package cn.thinkbear.app.running.base;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.qindachang.bluetoothle.BluetoothConfig;
import com.qindachang.bluetoothle.BluetoothLe;

import cn.thinkbear.app.running.interf.Permission;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * Created by linking on 3/16/18.
 */

public class BaseActivityBlueToothLE extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //蓝牙配置
        BluetoothConfig config = new BluetoothConfig.Builder()
                .enableQueueInterval(true)
                .setQueueIntervalTime(BluetoothConfig.AUTO)//发送时间间隔将根据蓝牙硬件自动得出
                .build();
        BluetoothLe.getDefault().init(this, config);
    }

    String[] locations = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

    protected boolean checkLocationPermission() {
        return EasyPermissions.hasPermissions(this, locations);
    }

    protected void requestLocationPermission() {
        EasyPermissions.requestPermissions(this, "Android 6.0以上扫描蓝牙需要该权限", Permission.LOCATION, locations);
    }
}
