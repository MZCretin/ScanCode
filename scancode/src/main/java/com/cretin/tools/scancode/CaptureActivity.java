package com.cretin.tools.scancode;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import cn.bingoogolapple.qrcode.core.QRCodeView;
import cn.bingoogolapple.qrcode.zbar.ZBarView;

/**
 * 邮箱: 1076559197@qq.com
 * <p>
 * 作者: 强迫症患者
 */
public class CaptureActivity extends AppCompatActivity implements QRCodeView.Delegate {
    private ZBarView mZBarView;
    public static final int REQUEST_CODE_SCAN = 0x0000;// 扫描二维码
    public static final String EXTRA_SCAN_RESULT = "extra_string";
    private static final int REQUEST_IMAGE = 111;
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 10001;
    private static final int MY_PERMISSIONS_REQUEST_STORAGE = 10002;
    private static final String PERMISSION_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    public static final int REQUEST_PERMISSION_SETTING = 1002;

    private TextView tvFlash;
    private TextView tvGallery;
    private boolean flashFlag = false;
    private ScanConfig globalConfig;

    //带配置打开页面
    public static void launch(Activity context, ScanConfig config) {
        if (context == null)
            return;
        Intent intent = new Intent(context, CaptureActivity.class);
        if (config != null)
            intent.putExtra("model", config);
        context.startActivityForResult(intent, REQUEST_CODE_SCAN);
    }

    public static void launch(Fragment context, ScanConfig config) {
        if (context == null)
            return;
        Intent intent = new Intent(context.getActivity(), CaptureActivity.class);
        if (config != null)
            intent.putExtra("model", config);
        context.startActivityForResult(intent, REQUEST_CODE_SCAN);
    }

    //不带配置打开页面
    public static void launch(Activity context) {
        launch(context, null);
    }

    //不带配置打开页面
    public static void launch(Fragment context) {
        launch(context, null);
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        setContentView(R.layout.activity_capture);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mZBarView = findViewById(R.id.zbarview);
        tvFlash = findViewById(R.id.tv_flash);
        tvGallery = findViewById(R.id.tv_galary);
        mZBarView.setDelegate(this);

        //对配置做解析
        globalConfig = getIntent().getParcelableExtra("model");
        if (globalConfig != null) {
            //用户自定义了配置
            //是否需要闪光灯
            if (!globalConfig.isShowFlashlight()) {
                findViewById(R.id.tv_flash).setVisibility(View.GONE);
            }
            //是否需要相册
            if (!globalConfig.isShowGalary()) {
                findViewById(R.id.scan_ll_galary_container).setVisibility(View.GONE);
            }
        } else {
            globalConfig = new ScanConfig();
        }

        tvFlash.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                flashFlag = !flashFlag;
                if (flashFlag) {
                    mZBarView.openFlashlight();
                    tvFlash.setText(getResources().getString(R.string.str_close_flash));
                } else {
                    mZBarView.closeFlashlight();
                    tvFlash.setText(getResources().getString(R.string.str_open_flash));
                }
            }
        });

        tvGallery.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermissionStorage();
            }
        });
    }

    /**
     * 打开系统相册
     */
    private void openGalary() {
        /*打开相册*/
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE);
    }

    //region 检查权限
    private void checkPermissionCamera() {
        if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.M)) {
            //6.0以下不需要申请权限
            mZBarView.startCamera(); // 打开后置摄像头开始预览，但是并未开始识别
            mZBarView.startSpotAndShowRect(); // 显示扫描框，并开始识别
        } else {
            //下载权限
            int writePermission = ContextCompat.checkSelfPermission(this, PERMISSION_CAMERA);
            if (writePermission == PackageManager.PERMISSION_GRANTED) {
                //拥有权限则直接操作
                mZBarView.startCamera(); // 打开后置摄像头开始预览，但是并未开始识别
                mZBarView.startSpotAndShowRect(); // 显示扫描框，并开始识别
            } else {
                // 申请权限
                ActivityCompat.requestPermissions(this, new String[]{PERMISSION_CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
            }
        }
    }

    private void checkPermissionStorage() {
        if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.M)) {
            //6.0以下不需要申请权限
            openGalary();
        } else {
            //下载权限
            int writePermission = ContextCompat.checkSelfPermission(this, PERMISSION_STORAGE);
            if (writePermission == PackageManager.PERMISSION_GRANTED) {
                //拥有权限则直接操作
                openGalary();
            } else {
                // 申请权限
                ActivityCompat.requestPermissions(this, new String[]{PERMISSION_STORAGE}, MY_PERMISSIONS_REQUEST_STORAGE);
            }
        }
    }

    private void openSettingPage() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, REQUEST_PERMISSION_SETTING);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkPermissionCamera();
    }

    @Override
    protected void onStop() {
        mZBarView.stopCamera(); // 关闭摄像头预览，并且隐藏扫描框
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mZBarView.onDestroy(); // 销毁二维码扫描控件
        super.onDestroy();
    }

    @Override
    public void onScanQRCodeSuccess(String result) {
        if (globalConfig.isNeedRing())
            VoiceUtil.playSound(this, R.raw.qrcode);

        activityResult(result);
    }

    private void activityResult(String result) {
        if (result == null) {
            Toast.makeText(this, getResources().getString(R.string.str_decode_fail), Toast.LENGTH_SHORT).show();
        }
        if (!isFinishing()) {
            Intent intent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putString(EXTRA_SCAN_RESULT, result);
            intent.putExtras(bundle);
            setResult(RESULT_OK, intent);
            CaptureActivity.this.finish();
        }
    }

    @Override
    public void onCameraAmbientBrightnessChanged(boolean isDark) {
        // 这里是通过修改提示文案来展示环境是否过暗的状态，接入方也可以根据 isDark 的值来实现其他交互效果
        String tipText = mZBarView.getScanBoxView().getTipText();
        String ambientBrightnessTip = getResources().getString(R.string.please_open_flash);
        if (isDark) {
            if (!tipText.contains(ambientBrightnessTip)) {
                mZBarView.getScanBoxView().setTipText(tipText + ambientBrightnessTip);
            }
        } else {
            if (tipText.contains(ambientBrightnessTip)) {
                tipText = tipText.substring(0, tipText.indexOf(ambientBrightnessTip));
                mZBarView.getScanBoxView().setTipText(tipText);
            }
        }
    }

    @Override
    public void onScanQRCodeOpenCameraError() {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        mZBarView.showScanRect();

        if (requestCode == REQUEST_IMAGE && resultCode == RESULT_OK) {
            String path = ImageUtil.getImageAbsolutePath(this, data.getData());
            mZBarView.decodeQRCode(path);
        }

        if (requestCode == REQUEST_PERMISSION_SETTING) {
            //设置页面回来了
            checkPermissionCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mZBarView.startCamera(); // 打开后置摄像头开始预览，但是并未开始识别
                    mZBarView.startSpotAndShowRect(); // 显示扫描框，并开始识别
                } else {
                    //授权拒绝
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, PERMISSION_CAMERA)) {
                        new AlertDialog.Builder(this)
                                .setTitle(this.getString(R.string.permissions_check_warn))
                                .setMessage(getResources().getString(R.string.permission))
                                .setCancelable(false)
                                .setPositiveButton(getResources().getString(R.string.permissions_check_ok), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        openSettingPage();
                                    }
                                }).show();
                    } else {
                        checkPermissionCamera();
                    }
                }
                break;
            }
            case MY_PERMISSIONS_REQUEST_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGalary();
                }
                break;
            }
        }
    }

}
