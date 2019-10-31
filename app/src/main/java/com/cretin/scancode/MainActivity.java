package com.cretin.scancode;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.cretin.tools.scancode.CaptureActivity;
import com.cretin.tools.scancode.config.ScanConfig;

import static com.cretin.tools.scancode.CaptureActivity.REQUEST_CODE_SCAN;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView btn_scan;
    private TextView tv_scanResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn_scan = (TextView) findViewById(R.id.btn_scan);
        tv_scanResult = (TextView) findViewById(R.id.tv_scanResult);
        btn_scan.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_scan:
                goScan();
                break;
            default:
                break;
        }
    }

    /**
     * 跳转到扫码界面扫码
     */
    private void goScan() {
        boolean isNeedFlashlight = ((CheckBox) findViewById(R.id.cb_flashlight)).isChecked();
        boolean isNeedGaraly = ((CheckBox) findViewById(R.id.cb_grlary)).isChecked();
        boolean isNeedRing = ((CheckBox) findViewById(R.id.cb_tips)).isChecked();
        ScanConfig config = new ScanConfig()
                .setShowFlashlight(isNeedFlashlight)//是否需要打开闪光灯
                .setShowGalary(isNeedGaraly)//是否需要打开相册
                .setNeedRing(isNeedRing);//是否需要提示音
        CaptureActivity.launch(this, config);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_SCAN:// 二维码
                // 扫描二维码回传
                if (resultCode == RESULT_OK) {
                    if (data != null) {
                        //获取扫描结果
                        Bundle bundle = data.getExtras();
                        String result = bundle.getString(CaptureActivity.EXTRA_SCAN_RESULT);
                        tv_scanResult.setText("扫描结果：" + result);
                    }
                }
                break;
            default:
                break;
        }
    }
}

