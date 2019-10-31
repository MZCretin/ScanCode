package com.cretin.tools.scancode;

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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cretin.tools.scancode.camera.CameraManager;
import com.cretin.tools.scancode.config.ScanConfig;
import com.cretin.tools.scancode.decode.MainHandler;
import com.cretin.tools.scancode.utils.BeepManager;
import com.cretin.tools.scancode.utils.ImageUtil;
import com.dtr.zbar.build.ZBarDecoder;

import java.io.IOException;
import java.lang.reflect.Field;

/**
 * Desc: 1:启动一个SurfaceView作为取景预览
 * 2:开启camera,在后台独立线程中完成扫描任务
 * 3:对解码返回的结果进行处理.
 * 4:释放资源
 */
public class CaptureActivity extends Activity implements SurfaceHolder.Callback {
    private static final String PERMISSION_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    public static final int REQUEST_CODE_SCAN = 0x0000;// 扫描二维码
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 1000001;
    private static final int MY_PERMISSIONS_REQUEST_STORAGE = 1000002;
    private static final int REQUEST_IMAGE = 1000001;
    public static final String EXTRA_SCAN_RESULT = "extra_string";
    private static final String TAG = "CaptureActivity";
    private MainHandler mainHandler;
    private SurfaceHolder mHolder;

    private CameraManager mCameraManager;
    private BeepManager beepManager;

    private SurfaceView scanPreview;
    private RelativeLayout scanContainer;
    private RelativeLayout scanCropView;
    private ImageView scanLine;
    private Rect mCropRect = null;

    private boolean isHasSurface = false;
    private boolean isOpenCamera = false;

    private ScanConfig globalConfig;

    public Handler getHandler() {
        return mainHandler;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        setContentView(R.layout.activity_capture);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        initView();
        checkPermissionCamera();
    }

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

    private void initView() {
        scanPreview = (SurfaceView) findViewById(R.id.capture_preview);
        scanContainer = (RelativeLayout) findViewById(R.id.capture_container);
        scanCropView = (RelativeLayout) findViewById(R.id.capture_crop_view);
        scanLine = (ImageView) findViewById(R.id.capture_scan_line);

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

        //返回
        findViewById(R.id.capture_imageview_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        isHasSurface = false;
        beepManager = new BeepManager(this);

        TranslateAnimation animation = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f, Animation
                .RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT,
                0.9f);
        animation.setDuration(3000);
        animation.setRepeatCount(-1);
        animation.setRepeatMode(Animation.RESTART);
        scanLine.startAnimation(animation);

        //打开相册
        findViewById(R.id.tv_galary).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermissionStorage();
            }
        });

        //打开闪光灯
        findViewById(R.id.tv_flash).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //  Log.i("打开闪光灯", "openFlashLight");
                boolean isOpen = mCameraManager.switchFlashState();
                if (isOpen) {
                    //显示轻触关闭
                    ((TextView) findViewById(R.id.tv_flash)).setText(getResources().getString(R.string.str_close_flash));
                } else {
                    //显示轻触打开
                    ((TextView) findViewById(R.id.tv_flash)).setText(getResources().getString(R.string.str_open_flash));
                }
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

    private void decodeFromGalary(String path) {
        if (TextUtils.isEmpty(path)) {
            activityResult(null);
            return;
        }

        ZBarDecoder zBarDecoder = new ZBarDecoder();
        Bitmap bitmap = BitmapFactory.decodeFile(path);
        bitmap = getSmallerBitmap(bitmap);
        if (bitmap != null) {
            int bitmapWidth = bitmap.getWidth();
            int bitmapHeight = bitmap.getHeight();
            // 1.将bitmap的RGB数据转化成YUV420sp数据
            byte[] bmpYUVBytes = getBitmapYUVBytes(bitmap);
            // 2.塞给zxing进行decode
            activityResult(zBarDecoder.decodeRaw(bmpYUVBytes, bitmapWidth, bitmapHeight));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isOpenCamera) {
            mHolder = scanPreview.getHolder();
            mCameraManager = new CameraManager(getApplication());
            beepManager = new BeepManager(this);
            if (isHasSurface) {
                initCamera(mHolder);
            } else {
                mHolder.addCallback(this);
                mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            }
        }
    }

    @Override
    public void onPause() {
        releaseCamera();
        super.onPause();
        if (scanLine != null) {
            scanLine.clearAnimation();
            scanLine.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //remove SurfaceCallback
        if (!isHasSurface) {
            scanPreview.getHolder().removeCallback(this);
        }
    }

    //region 初始化和回收相关资源
    private void initCamera(SurfaceHolder surfaceHolder) {
        mainHandler = null;
        try {
            mCameraManager.openDriver(surfaceHolder);
            if (mainHandler == null) {
                mainHandler = new MainHandler(this, mCameraManager);
            }
        } catch (IOException ioe) {
            Log.e(TAG, "相机被占用", ioe);
        } catch (RuntimeException e) {
            e.printStackTrace();
            Log.e(TAG, "Unexpected error initializing camera");
            displayFrameworkBugMessageAndExit();
        }
    }

    private void releaseCamera() {
        if (null != mainHandler) {
            //关闭聚焦,停止预览,清空预览回调,quit子线程looper
            mainHandler.quitSynchronously();
            mainHandler = null;
        }
        //关闭声音
        if (null != beepManager) {
            Log.e(TAG, "releaseCamera: beepManager release");
            beepManager.releaseRing();
            beepManager = null;
        }
        //关闭相机
        if (mCameraManager != null) {
            mCameraManager.closeDriver();
            mCameraManager = null;
        }
    }
    //endregion

    //region 检查权限
    private void checkPermissionCamera() {
        if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.M)) {
            //6.0以下不需要申请权限
            isOpenCamera = true;
        } else {
            //下载权限
            int writePermission = ContextCompat.checkSelfPermission(this, PERMISSION_CAMERA);
            if (writePermission == PackageManager.PERMISSION_GRANTED) {
                //拥有权限则直接操作
                isOpenCamera = true;
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE && resultCode == RESULT_OK) {
            String path = ImageUtil.getImageAbsolutePath(this, data.getData());
            decodeFromGalary(path);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    isHasSurface = true;
                    isOpenCamera = true;
                    onResume();
                } else {
                    checkPermissionCamera();
                }
                break;
            }
            case MY_PERMISSIONS_REQUEST_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGalary();
                } else {
                    checkPermissionStorage();
                }
                break;
            }
        }
    }

    private void displayFrameworkBugMessageAndExit() {
        String per = String.format(getString(R.string.permission), getString(R.string.camera), getString(R.string.camera));
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.str_scan));
        builder.setMessage(per);
        builder.setPositiveButton(getString(R.string.i_know), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                checkPermissionCamera();
            }

        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                CaptureActivity.this.finish();
            }
        });
        builder.show();
    }
    //endregion

    //region 扫描结果
    public void checkResult(final String result) {
        if (beepManager != null) {
            if (globalConfig.isNeedRing())
                beepManager.startRing();
        }
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                activityResult(result.trim());
            }
        }, beepManager.getTimeDuration());
    }

    private void activityResult(String result) {
        if (result == null) {
            Toast.makeText(this, getResources().getString(R.string.str_decode_fail), Toast.LENGTH_SHORT).show();
        }
        if (!isFinishing()) {
            Intent intent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putInt("width", mCropRect.width());
            bundle.putInt("height", mCropRect.height());
            bundle.putString(EXTRA_SCAN_RESULT, result);
            intent.putExtras(bundle);
            setResult(RESULT_OK, intent);
            CaptureActivity.this.finish();
        }
    }

    //endregion

    //region  初始化截取的矩形区域
    public Rect initCrop() {
        int cameraWidth = 0;
        int cameraHeight = 0;
        if (null != mCameraManager) {
            cameraWidth = mCameraManager.getCameraResolution().y;
            cameraHeight = mCameraManager.getCameraResolution().x;
        }

        /** 获取布局中扫描框的位置信息 */
        int[] location = new int[2];
        scanCropView.getLocationInWindow(location);

        int cropLeft = location[0];
        int cropTop = location[1] - getStatusBarHeight();

        int cropWidth = scanCropView.getWidth();
        int cropHeight = scanCropView.getHeight();

        /** 获取布局容器的宽高 */
        int containerWidth = scanContainer.getWidth();
        int containerHeight = scanContainer.getHeight();

        /** 计算最终截取的矩形的左上角顶点x坐标 */
        int x = cropLeft * cameraWidth / containerWidth;
        /** 计算最终截取的矩形的左上角顶点y坐标 */
        int y = cropTop * cameraHeight / containerHeight;

        /** 计算最终截取的矩形的宽度 */
        int width = cropWidth * cameraWidth / containerWidth;
        /** 计算最终截取的矩形的高度 */
        int height = cropHeight * cameraHeight / containerHeight;

        /** 生成最终的截取的矩形 */
        mCropRect = new Rect(x, y, width + x, height + y);
        return new Rect(x, y, width + x, height + y);
    }

    private int getStatusBarHeight() {
        try {
            Class<?> c = Class.forName("com.android.internal.R$dimen");
            Object obj = c.newInstance();
            Field field = c.getField("status_bar_height");
            int x = Integer.parseInt(field.get(obj).toString());
            return getResources().getDimensionPixelSize(x);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
    //endregion

    //region SurfaceHolder Callback 回调方法
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            Log.e(TAG, "*** 没有添加SurfaceHolder的Callback");
        }
        if (isOpenCamera) {
            if (!isHasSurface) {
                isHasSurface = true;
                initCamera(holder);
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.e(TAG, "surfaceChanged: ");

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isHasSurface = false;

    }
    //endregion

    private static Bitmap getSmallerBitmap(Bitmap bitmap) {
        int size = bitmap.getWidth() * bitmap.getHeight() / 160000;
        if (size <= 1) return bitmap; // 如果小于
        else {
            Matrix matrix = new Matrix();
            matrix.postScale((float) (1 / Math.sqrt(size)), (float) (1 / Math.sqrt(size)));
            Bitmap resizeBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            return resizeBitmap;
        }
    }

    private static byte[] getBitmapYUVBytes(Bitmap sourceBmp) {
        if (null != sourceBmp) {
            int inputWidth = sourceBmp.getWidth();
            int inputHeight = sourceBmp.getHeight();
            int[] argb = new int[inputWidth * inputHeight];
            sourceBmp.getPixels(argb, 0, inputWidth, 0, 0, inputWidth, inputHeight);
            byte[] yuv = new byte[inputWidth
                    * inputHeight
                    + ((inputWidth % 2 == 0 ? inputWidth : (inputWidth + 1)) * (inputHeight % 2 == 0 ? inputHeight
                    : (inputHeight + 1))) / 2];
            encodeYUV420SP(yuv, argb, inputWidth, inputHeight);
            sourceBmp.recycle();
            return yuv;
        }
        return null;
    }

    /**
     * 将bitmap里得到的argb数据转成yuv420sp格式
     * 这个yuv420sp数据就可以直接传给MediaCodec, 通过AvcEncoder间接进行编码
     *
     * @param yuv420sp 用来存放yuv429sp数据
     * @param argb     传入argb数据
     * @param width    bmpWidth
     * @param height   bmpHeight
     */
    private static void encodeYUV420SP(byte[] yuv420sp, int[] argb, int width, int height) {
        // 帧图片的像素大小
        final int frameSize = width * height;
        // Y的index从0开始
        int yIndex = 0;
        // UV的index从frameSize开始
        int uvIndex = frameSize;
        // YUV数据, ARGB数据
        int Y, U, V, a, R, G, B;
        ;
        int argbIndex = 0;
        // ---循环所有像素点，RGB转YUV---
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {

                // a is not used obviously
                a = (argb[argbIndex] & 0xff000000) >> 24;
                R = (argb[argbIndex] & 0xff0000) >> 16;
                G = (argb[argbIndex] & 0xff00) >> 8;
                B = (argb[argbIndex] & 0xff);
                argbIndex++;

                // well known RGB to YUV algorithm
                Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
                U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
                V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;

                Y = Math.max(0, Math.min(Y, 255));
                U = Math.max(0, Math.min(U, 255));
                V = Math.max(0, Math.min(V, 255));

                // NV21 has a plane of Y and interleaved planes of VU each
                // sampled by a factor of 2
                // meaning for every 4 Y pixels there are 1 V and 1 U. Note the
                // sampling is every other
                // pixel AND every other scanline.
                // ---Y---
                yuv420sp[yIndex++] = (byte) Y;
                // ---UV---
                if ((j % 2 == 0) && (i % 2 == 0)) {
                    yuv420sp[uvIndex++] = (byte) V;
                    yuv420sp[uvIndex++] = (byte) U;
                }
            }
        }
    }
}
