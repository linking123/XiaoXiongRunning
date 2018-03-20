package cn.thinkbear.app.running.activity;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.TextView;
import android.widget.Toast;

import com.qindachang.bluetoothle.BluetoothLe;
import com.qindachang.bluetoothle.OnLeConnectListener;
import com.qindachang.bluetoothle.OnLeIndicationListener;
import com.qindachang.bluetoothle.OnLeNotificationListener;
import com.qindachang.bluetoothle.OnLeReadCharacteristicListener;
import com.qindachang.bluetoothle.OnLeReadRssiListener;
import com.qindachang.bluetoothle.OnLeScanListener;
import com.qindachang.bluetoothle.OnLeWriteCharacteristicListener;
import com.qindachang.bluetoothle.exception.BleException;
import com.qindachang.bluetoothle.exception.ConnBleException;
import com.qindachang.bluetoothle.exception.ReadBleException;
import com.qindachang.bluetoothle.exception.ScanBleException;
import com.qindachang.bluetoothle.exception.WriteBleException;
import com.qindachang.bluetoothle.scanner.ScanRecord;
import com.qindachang.bluetoothle.scanner.ScanResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Exchanger;

import cn.thinkbear.app.running.R;

import cn.thinkbear.app.running.App;
import cn.thinkbear.app.running.base.BaseActivityBlueToothLE;
import cn.thinkbear.app.running.interf.BluetoothUUID;
import cn.thinkbear.app.running.thread.MyRunnable;
import cn.thinkbear.app.running.utils.ApiLevelHelper;
import cn.thinkbear.app.running.utils.LocationUtils;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

/**
 * 游戏主界面
 *
 * @author Linking
 */
public class RunningGameActivity extends BaseActivityBlueToothLE implements
        SurfaceHolder.Callback, OnTouchListener {

    public static final String TAG = "RunningGameActivity";

    private SurfaceView main = null;
    private MyRunnable run = null;
    private MaterialProgressBar progressBar = null;
    private TextView tvLoading = null;
    private TextView timeStatu = null;
    private TextView gameStatu = null;
    private MyClickEvent myClickEvent = null;

    private int gameType = 0;
    private Handler myHandler = null;

    //蓝牙对象
    private BluetoothLe mBluetoothLe;
    private BluetoothDevice mBluetoothDevice;
    private List<BluetoothDevice> bluetoothDeviceList = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.main);
        if (savedInstanceState != null) {
            this.gameType = savedInstanceState.getInt(App.GAMETYPE);
        } else {
            this.gameType = super.getIntent().getIntExtra(App.GAMETYPE, 0);
        }
        doInitView();
        doSetView();

        mBluetoothLe = BluetoothLe.getDefault();
        checkSupport();
        registerListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //根据TAG注销监听，避免内存泄露
        mBluetoothLe.destroy(TAG);
        //关闭GATT
        mBluetoothLe.close();
    }

    public void checkSupport() {
        //初始检测设备是否支持蓝牙
        if (!mBluetoothLe.isSupportBluetooth()) {
            //设备不支持蓝牙
            Toast.makeText(getApplicationContext(), "很遗憾，您的手机不支持蓝牙4.0及以上，请更换手机后重试", Toast.LENGTH_SHORT).show();
        } else {
            if (!mBluetoothLe.isBluetoothOpen()) {
                //没有打开蓝牙，请求打开手机蓝牙
                mBluetoothLe.enableBluetooth(this, 666);
            } else {
                scan();
            }
        }
    }

    //byte数组转化为16进制hex
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    //判断扫描到的蓝牙是否已经存在列表中
    private boolean isNotInBluetoothDeviceList(BluetoothDevice mBluetoothDevice) {
        for (BluetoothDevice btd : bluetoothDeviceList) {
            if (mBluetoothDevice.equals(btd)) {
                return false;
            }
        }
        return true;
    }

    private void scan() {
        //对于Android 6.0以上的版本，申请地理位置动态权限
        if (!checkLocationPermission()) {
            android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
            builder.setTitle("权限需求")
                    .setMessage("Android 6.0 以上的系统版本，扫描蓝牙需要地理位置权限。请允许。")
                    .setNeutralButton("取消", null)
                    .setPositiveButton("确认", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestLocationPermission();
                        }
                    })
                    .show();
            return;
        }
        //如果系统版本是7.0以上，则请求打开位置信息
        if (!LocationUtils.isOpenLocService(this) && ApiLevelHelper.isAtLeast(Build.VERSION_CODES.N)) {
            Toast.makeText(this, "您的Android版本在7.0以上，扫描需要打开位置信息。", Toast.LENGTH_LONG).show();
            LocationUtils.gotoLocServiceSettings(this);
            return;
        }
        mBluetoothLe.setScanPeriod(200000)
                //   .setScanWithServiceUUID(BluetoothUUID.SERVICE)
                .setReportDelay(0)
                .startScan(this);
    }

    private void registerListener() {

        //监听蓝牙回调
        //监听扫描
        mBluetoothLe.setOnScanListener(TAG, new OnLeScanListener() {
            @Override
            public void onScanResult(BluetoothDevice bluetoothDevice, int rssi, ScanRecord scanRecord) {
                mBluetoothDevice = bluetoothDevice;
                //不存在，则添加进蓝牙设备列表
                if (bluetoothDeviceList.size() == 0) {
                    bluetoothDeviceList.add(bluetoothDevice);
                } else if (isNotInBluetoothDeviceList(bluetoothDevice)) {
                    bluetoothDeviceList.add(bluetoothDevice);
                }
                Log.i(TAG, "扫描到设备：" + mBluetoothDevice.getName());

//                if ("BT05".equals(mBluetoothDevice.getName())) {
                mBluetoothLe.startConnect(true, mBluetoothDevice);
//                }
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                Log.i(TAG, "扫描到设备：" + results.toString());
            }

            @Override
            public void onScanCompleted() {
                Log.i(TAG, "停止扫描");
            }

            @Override
            public void onScanFailed(ScanBleException e) {
                Log.e(TAG, "扫描错误：" + e.toString());
            }
        });

        //监听连接
        mBluetoothLe.setOnConnectListener(TAG, new OnLeConnectListener() {
            @Override
            public void onDeviceConnecting() {
                Log.i(TAG, "正在连接--->：" + mBluetoothDevice.getAddress());
            }

            @Override
            public void onDeviceConnected() {
                Log.i(TAG, "成功连接，开始游戏！");
            }

            @Override
            public void onDeviceDisconnected() {
                Log.i(TAG, "连接断开！");
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt) {
                Log.i(TAG, "发现蓝牙服务，开始游戏");

                //写之前打开通知，以监听通知
                mBluetoothLe.enableNotification(true, BluetoothUUID.psServiceUUID,
                        new UUID[]{BluetoothUUID.PS_HR_NOTIFICATION, BluetoothUUID.PS_STEP_NOTIFICATION});

                //发送数据等必须在发现服务后做
                String writeStr = "0xA5F1010097";
                mBluetoothLe.writeDataToCharacteristic(writeStr.getBytes(),
                        BluetoothUUID.psServiceUUID, BluetoothUUID.psWriteUUID);

                //读数据
                mBluetoothLe.readCharacteristic(BluetoothUUID.psServiceUUID, BluetoothUUID.psReadUUID);

                tvLoading.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getApplicationContext(), "服务已链接，愉快的玩耍吧！", Toast.LENGTH_SHORT).show();

                try {
                    Thread.sleep(1000);
                    run.setPause(false);
//                    gameStatu.setText(run.isPause() ? R.string.conti : R.string.pause);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onDeviceConnectFail(ConnBleException e) {
                Log.e(TAG, "连接异常：" + e.toString());
            }
        });

        //监听通知，类型notification
        mBluetoothLe.setOnNotificationListener(TAG, new OnLeNotificationListener() {
            @Override
            public void onSuccess(BluetoothGattCharacteristic characteristic) {
//                Log.i(TAG, "收到notification : " + Arrays.toString(characteristic.getValue()));
                //16进制数
                String backInfo = bytesToHex(characteristic.getValue());
                Log.i(TAG, "收到notification : " + backInfo);

                if (backInfo.length() < 8) {
                    return;
                }

                // 十六进制转化为十进制
                //根据测试说明：机器周期性发送压力值数据指令（A5 F1 03 00 00 00），
                // 目前只需要用到第4位数据即蓝色那位。 其值的大小，会根据按压气囊的值而相应改变。
                // 所以需要得到第四部分，转化为10进制，然后在改变高度
                String the4Num = bytesToHex(characteristic.getValue()).substring(6, 8);

                Log.i("the4num is", the4Num);
                int pressureNum = Integer.parseInt(the4Num, 16);  //转化为10进制的压力值,最大值120左右
                Log.i("pressureNum", String.valueOf(pressureNum));

                if (run != null && pressureNum > 0) {//绘制线程对象已存在
                    Log.i("success", "握力数据接收成功");
                    switch (run.getGameStatu()) {
                        case MyRunnable.RUNNING_STATU://当前的游戏状态为在跑中
                            run.setJump(true);//设置跳跃标记为true
                            break;
                    }
                }

                //改变图形的高度
//                changeHeight(pressureNum);
            }

            @Override
            public void onFailed(BleException e) {
                Log.e(TAG, "notification通知错误：" + e.toString());
            }
        });

        //监听通知，类型indicate
        mBluetoothLe.setOnIndicationListener(TAG, new OnLeIndicationListener() {
            @Override
            public void onSuccess(BluetoothGattCharacteristic characteristic) {
                Log.i(TAG, "收到indication: " + Arrays.toString(characteristic.getValue()));
            }

            @Override
            public void onFailed(BleException e) {
                Log.e(TAG, "indication通知错误：" + e.toString());
            }
        });

        //监听写
        mBluetoothLe.setOnWriteCharacteristicListener(TAG, new OnLeWriteCharacteristicListener() {
            @Override
            public void onSuccess(BluetoothGattCharacteristic characteristic) {
                Log.i(TAG, "写数据到特征：" + Arrays.toString(characteristic.getValue()));
            }

            @Override
            public void onFailed(WriteBleException e) {
                Log.e(TAG, "写错误：" + e.toString());
            }
        });

        //监听读
        mBluetoothLe.setOnReadCharacteristicListener(TAG, new OnLeReadCharacteristicListener() {
            @Override
            public void onSuccess(BluetoothGattCharacteristic characteristic) {
                Log.i(TAG, "读取特征数据：" + Arrays.toString(characteristic.getValue()));
                String returnstr = bytesToHex(characteristic.getValue());
                Log.i(TAG, "读取特征数据：" + returnstr);
            }

            @Override
            public void onFailure(ReadBleException e) {
                Log.e(TAG, "读错误：" + e.toString());
            }
        });

        //监听信号强度
        mBluetoothLe.setReadRssiInterval(10000)
                .setOnReadRssiListener(TAG, new OnLeReadRssiListener() {
                    @Override
                    public void onSuccess(int rssi, int cm) {
                        Log.i(TAG, "信号强度：" + rssi + "   距离：" + cm + "cm");
                    }
                });
    }

    private void changeHeight(int pressureNum) {
//        Random rand = new Random();
//        int height = rand.nextInt(400);
//        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mPressure.getLayoutParams();
//        params.height = 3 * pressureNum;
//        mPressure.setLayoutParams(params);
//        mNumText.setText(String.valueOf(params));

        Log.i("pressureNum", pressureNum + "");

        if (this.run != null && pressureNum > 0) {//绘制线程对象已存在
            Log.i("success", "握力数据接收成功");
            switch (this.run.getGameStatu()) {
                case MyRunnable.RUNNING_STATU://当前的游戏状态为在跑中
                    run.setJump(true);//设置跳跃标记为true
                    break;
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(App.GAMETYPE, this.gameType);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {//用户点击了返回按钮
        run.setPause(true);//先暂定游戏，并弹出对话框
        new AlertDialog.Builder(this).setTitle(R.string.app_name)
                .setMessage("确定要返回主菜单？")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(RunningGameActivity.this,
                                StartActivity.class));//跳转到主菜单页面
                        finish();
                    }
                })
                .setNegativeButton("继续游戏",
                        new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog,
                                                int which) {
                                run.setPause(false);//继续游戏
                            }
                        }).show();
    }

    private void doInitView() {
        this.main = super.findViewById(R.id.main);

        this.progressBar = super.findViewById(R.id.progressBar);
        this.tvLoading = super.findViewById(R.id.tv_loading);
        this.timeStatu = super.findViewById(R.id.timeStatu);
        this.gameStatu = super.findViewById(R.id.gameStatu);

        this.main.getHolder().addCallback(this);
        this.myClickEvent = new MyClickEvent();
        this.myHandler = new Handler(new Handler.Callback() {

            public boolean handleMessage(Message msg) {
                Intent intent = new Intent(RunningGameActivity.this,
                        GameOverActivity.class);
                intent.putExtra(App.GAMETIME, run.getGameTime());//取得游戏的总时长
                intent.putExtra(App.PASSCOUNT, run.getPassCount());//取得跳跃的柱子数
                intent.putExtra(App.GAMETYPE, gameType);//取得游戏的难度值
                startActivity(intent);//开始跳转
                finish();//结束本页面
                return false;
            }
        });
    }

    private void doSetView() {
        this.main.setOnTouchListener(this);
        this.timeStatu.setOnClickListener(this.myClickEvent);
        this.gameStatu.setOnClickListener(this.myClickEvent);
    }

    private class MyClickEvent implements OnClickListener {

        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.timeStatu://修改夜间或白天模式
                    if (run.getColor() == Color.BLACK) {// 当前为夜间模式
                        run.setColor(Color.WHITE);
                        timeStatu.setText(R.string.night);
                    } else {//
                        run.setColor(Color.BLACK);
                        timeStatu.setText(R.string.day);
                    }
                    break;
                case R.id.gameStatu://暂定或继续游戏操作
                    run.setPause(!run.isPause());
                    gameStatu.setText(run.isPause() ? R.string.conti
                            : R.string.pause);
                    break;
            }
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {//SurfaceView的回调方法
        this.run = new MyRunnable(super.getApplicationContext(),
                this.main.getHolder(), this.myHandler);//初始化绘制的线程对象
        this.run.setPlaying(true);//设置开始游戏标记为true
        this.run.setGameType(this.gameType);//设置好游戏的难度
        new Thread(this.run).start();//开始绘制
        try {
            Thread.sleep(100);
            this.run.setPause(true);
//            gameStatu.setText(run.isPause() ? R.string.conti : R.string.pause);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        if (this.run != null) {
            this.run.setPlaying(false);
        }
    }

    public boolean onTouch(View v, MotionEvent event) {
        if (this.run != null) {//绘制线程对象已存在
            switch (this.run.getGameStatu()) {
                case MyRunnable.RUNNING_STATU://当前的游戏状态为在跑中
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {//用户点击了屏幕
                        run.setJump(true);//设置跳跃标记为true
                    }
                    break;
            }
        }

        return false;
    }

}