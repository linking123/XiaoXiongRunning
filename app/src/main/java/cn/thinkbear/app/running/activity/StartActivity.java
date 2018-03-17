package cn.thinkbear.app.running.activity;

import android.app.Activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.qindachang.bluetoothle.BluetoothLe;

import java.util.Calendar;

import cn.thinkbear.app.running.App;
import cn.thinkbear.app.running.R;
import cn.thinkbear.app.running.base.BaseActivityBlueToothLE;

/**
 * 游戏的开始菜单页，用户可选择难度和退出游戏操作
 *
 * @author Linking
 */
public class StartActivity extends BaseActivityBlueToothLE {

    public static final String TAG = "StartActivity";

    private TextView one = null;
    private TextView two = null;
    private TextView three = null;
    private TextView copyRight = null;
    private ImageView logo = null;
    private AnimationDrawable ad = null;
    private MyClickEvent myClickEvent = null;

    //蓝牙对象
    private BluetoothLe mBluetoothLe;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start);
        this.one = super.findViewById(R.id.one);
        this.two = super.findViewById(R.id.two);
        this.three = super.findViewById(R.id.three);
        this.copyRight = super.findViewById(R.id.copyRight);
        this.logo = super.findViewById(R.id.logo);
        this.ad = (AnimationDrawable) this.logo.getBackground();
        this.myClickEvent = new MyClickEvent();
        this.one.setOnClickListener(this.myClickEvent);
        this.two.setOnClickListener(this.myClickEvent);
        this.three.setOnClickListener(this.myClickEvent);

        this.copyRight.setText(super.getString(R.string.copyRight, String.valueOf(Calendar.getInstance().get(Calendar.YEAR))));

        mBluetoothLe = BluetoothLe.getDefault();
        checkSupport();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

//        //根据TAG注销监听，避免内存泄露
//        mBluetoothLe.destroy(TAG);
////        //关闭GATT，只在activity中
//        mBluetoothLe.close();
    }

    /**
     * 检测是否支持蓝牙
     */
    public void checkSupport() {
        //初始检测设备是否支持蓝牙
        if (!mBluetoothLe.isSupportBluetooth()) {
            //设备不支持蓝牙
            Toast.makeText(getApplicationContext(), "很遗憾，您的手机不支持蓝牙4.0及以上，请更换手机后重试", Toast.LENGTH_SHORT).show();
        } else {
            if (!mBluetoothLe.isBluetoothOpen()) {
                //没有打开蓝牙，请求打开手机蓝牙
                mBluetoothLe.enableBluetooth(this, 666);
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            this.ad.start();
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this).setTitle(R.string.app_name)
                .setMessage("确定要退出游戏？")
                .setPositiveButton("退出",
                        new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog,
                                                int which) {
                                finish();
                            }
                        }).setNegativeButton("取消", null).show();
    }

    private class MyClickEvent implements OnClickListener {

        public void onClick(View v) {

            int gameType = -1;
            switch (v.getId()) {
                case R.id.one:
                    gameType = App.GAMETYPE_ONE;
                    break;
                case R.id.two:
                    gameType = App.GAMETYPE_TWO;
                    break;
                case R.id.three:
                    gameType = App.GAMETYPE_THREE;
                    break;

            }
            if (gameType != -1) {
                Intent intent = new Intent(StartActivity.this,
                        RunningGameActivity.class);
                intent.putExtra(App.GAMETYPE, gameType);
                startActivity(intent);
                finish();
            }

        }
    }
}
