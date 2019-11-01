package com.cretin.scancode.fragment;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.cretin.scancode.R;
import com.cretin.tools.scancode.CaptureActivity;
import com.cretin.tools.scancode.config.ScanConfig;

import static android.app.Activity.RESULT_OK;
import static com.cretin.tools.scancode.CaptureActivity.REQUEST_CODE_SCAN;

/**
 * A simple {@link Fragment} subclass.
 */
public class DemoFragment extends Fragment implements View.OnClickListener {
    private TextView btn_scan;
    private TextView tv_scanResult;
    private CheckBox cb_flashlight;
    private CheckBox cb_grlary;
    private CheckBox cb_tips;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_demo, container, false);
        btn_scan = (TextView) view.findViewById(R.id.btn_scan);
        tv_scanResult = (TextView) view.findViewById(R.id.tv_scanResult);
        cb_flashlight = view.findViewById(R.id.cb_flashlight);
        cb_grlary = view.findViewById(R.id.cb_grlary);
        cb_tips = view.findViewById(R.id.cb_tips);
        btn_scan.setOnClickListener(this);

        return view;
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
        boolean isNeedFlashlight = cb_flashlight.isChecked();
        boolean isNeedGaraly = cb_grlary.isChecked();
        boolean isNeedRing = cb_tips.isChecked();
        ScanConfig config = new ScanConfig()
                .setShowFlashlight(isNeedFlashlight)//是否需要打开闪光灯
                .setShowGalary(isNeedGaraly)//是否需要打开相册
                .setNeedRing(isNeedRing);//是否需要提示音
        CaptureActivity.launch(this, config);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CaptureActivity.REQUEST_CODE_SCAN) {
            // 扫描二维码回传
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    //获取扫描结果
                    Bundle bundle = data.getExtras();
                    String result = bundle.getString(CaptureActivity.EXTRA_SCAN_RESULT);
                    tv_scanResult.setText("扫描结果：" + result);
                }
            }
        }
    }
}
