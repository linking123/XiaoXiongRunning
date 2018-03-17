package cn.thinkbear.app.running;

import android.app.Application;

import com.qindachang.bluetoothle.BluetoothConfig;
import com.qindachang.bluetoothle.BluetoothLe;

public class App extends Application{
    public static final String GAMETYPE = "GAMETYPE";
    public static final int GAMETYPE_ONE = 0;
    public static final int GAMETYPE_TWO = 1;
    public static final int GAMETYPE_THREE = 2;
    public static final String GAMETIME = "GAMETIME";
    public static final String PASSCOUNT = "PASSCOUNT";

    @Override
    public void onCreate() {
        super.onCreate();

        //蓝牙配置
        BluetoothConfig config = new BluetoothConfig.Builder()
                .enableQueueInterval(true)
                .setQueueIntervalTime(BluetoothConfig.AUTO)//发送时间间隔将根据蓝牙硬件自动得出
                .build();
        BluetoothLe.getDefault().init(this, config);

    }
}
